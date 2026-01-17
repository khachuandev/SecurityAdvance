package com.example.SecurityAdvance.services.impl;

import com.example.SecurityAdvance.dtos.request.RefreshTokenDto;
import com.example.SecurityAdvance.dtos.request.UserLoginDto;
import com.example.SecurityAdvance.dtos.request.UserRegisterRequest;
import com.example.SecurityAdvance.dtos.response.LoginResponse;
import com.example.SecurityAdvance.dtos.response.UserInfoResponse;
import com.example.SecurityAdvance.dtos.response.UserResponse;
import com.example.SecurityAdvance.entities.EncryptionKey;
import com.example.SecurityAdvance.entities.Role;
import com.example.SecurityAdvance.entities.User;
import com.example.SecurityAdvance.enums.EncryptionKeyStatus;
import com.example.SecurityAdvance.enums.UserStatus;
import com.example.SecurityAdvance.exceptions.AppException;
import com.example.SecurityAdvance.exceptions.EmailAlreadyVerifiedException;
import com.example.SecurityAdvance.exceptions.TokenExpiredException;
import com.example.SecurityAdvance.exceptions.UserNotFoundException;
import com.example.SecurityAdvance.repositories.EncryptionKeyRepository;
import com.example.SecurityAdvance.repositories.RoleRepository;
import com.example.SecurityAdvance.repositories.UserRepository;
import com.example.SecurityAdvance.security.CustomUserDetails;
import com.example.SecurityAdvance.security.CustomUserDetailsService;
import com.example.SecurityAdvance.security.jwt.JwtTokenProvider;
import com.example.SecurityAdvance.services.IAuthService;
import com.example.SecurityAdvance.services.IEmailService;
import com.example.SecurityAdvance.services.IRedisService;
import com.example.SecurityAdvance.utils.AESUtils;
import com.example.SecurityAdvance.utils.HashUtils;
import com.example.SecurityAdvance.utils.RSAUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EncryptionKeyRepository encryptionKeyRepository;
    private final IRedisService redisService;
    private final IEmailService emailService;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final HashUtils hashUtils;
    private final AESUtils aesUtils;
    private final RSAUtils rsaUtils;

    @Override
    @Transactional
    public UserResponse register(UserRegisterRequest request) {
        if (!request.getPassword().equals(request.getRetypePassword())) {
            throw new IllegalArgumentException("Password không khớp");
        }

        // Check email tồn tại (plain text)
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException("err.duplicate.email", HttpStatus.CONFLICT);
        }

        /* ========== 1. GEN KEYS ========== */
        SecretKey dek = aesUtils.generateAESKey(); // Data Encryption Key
        SecretKey kek = aesUtils.generateAESKey(); // Key Encryption Key
        byte[] dataIv = aesUtils.generateIV();
        byte[] keyIv = aesUtils.generateIV();

        /* ========== 2. ENCRYPT PII ========== */
        String encryptedPhone = aesUtils.encrypt(request.getPhoneNumber(), dek, dataIv);
        String phoneHash = hashUtils.sha256(request.getPhoneNumber());

        /* ========== 3. WRAP DEK (AES) ========== */
        String dekBase64 = aesUtils.encodeKeyBase64(dek);
        String encryptedDek = aesUtils.encrypt(dekBase64, kek, keyIv);

        /* ========== 4. WRAP KEK (RSA) ========== */
        String kekBase64 = aesUtils.encodeKeyBase64(kek);
        String encryptedKek = rsaUtils.encryptAESKey(kekBase64);

        /* ========== 5. CREATE USER ========== */
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .phoneNumber(encryptedPhone)
                .phoneHash(phoneHash)
                .dataIv(Base64.getEncoder().encodeToString(dataIv))
                .status(UserStatus.PENDING_VERIFICATION)
                .build();

        /* ========== 6. Assign ROLE_USER ========== */
        Role roleUser = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("ROLE_USER chưa tồn tại"));

        user.addRole(roleUser);

        userRepository.save(user);

        /* ========== 7. SAVE ENCRYPTION KEY ========== */
        EncryptionKey encryptionKey = EncryptionKey.builder()
                .user(user)
                .keyName("USER_PII_KEY")
                .encryptedDek(encryptedDek)
                .encryptedKek(encryptedKek)
                .keyIv(Base64.getEncoder().encodeToString(keyIv))
                .status(EncryptionKeyStatus.ACTIVE)
                .build();

        encryptionKeyRepository.save(encryptionKey);

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        if (!redisService.isTokenValid(token)) {
            throw new TokenExpiredException();
        }

        Long userId = redisService.getUserIdByToken(token);
        if (userId == null) {
            throw new TokenExpiredException();
        }

        // 3. Lấy user
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if(user.getStatus() == UserStatus.ACTIVE) {
            throw new EmailAlreadyVerifiedException();
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        redisService.deleteToken(token);
    }

    @Override
    public void resendVerificationEmail(String email, String appUrl) {
        log.info("Resend verification email request for: {}", email);

        try {
            // 1. Tìm user
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));

            log.info("Found user: id={}, status={}", user.getId(), user.getStatus());

            // 2. Kiểm tra trạng thái
            if (user.getStatus() == UserStatus.ACTIVE) {
                throw new IllegalStateException("Email đã được xác thực");
            }

            // 3. Xóa token cũ
            log.info("Deleting old tokens for user: {}", user.getId());
            redisService.deleteAllUserTokens(user.getId());

            // 4. Tạo token mới
            String verificationToken = UUID.randomUUID().toString();
            log.info("Generated new token: {}", verificationToken);
            redisService.saveVerificationToken(verificationToken, user.getId());

            // 5. Gửi email
            String verificationUrl = appUrl + "/api/v1/auth/verify-email?token=" + verificationToken;
            log.info("Sending verification email to: {}", user.getEmail());
            emailService.sendVerificationEmailAsync(user, verificationUrl);

            log.info("Resend verification email completed successfully");
        } catch (Exception e) {
            log.error("Error in resendVerificationEmail: ", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public LoginResponse login(UserLoginDto request) {
        CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService
                .loadUserByUsername(request.getUsername());

        if(!passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if (userDetails.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Invalid user status");
        }

        String accessToken = jwtTokenProvider.generateToken(userDetails);
        String refreshToken = UUID.randomUUID().toString();

        redisService.saveRefreshToken(refreshToken, userDetails.getUser().getId());

        UserInfoResponse user = UserInfoResponse.builder()
                .id(userDetails.getUser().getId())
                .username(userDetails.getUser().getUsername())
                .email(userDetails.getUser().getEmail())
                .roles(userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet())
                )
                .status(UserStatus.ACTIVE)
                .build();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(user)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse refreshToken(RefreshTokenDto request) {
        String oldRefreshToken = request.getRefreshToken();

        Long userId = redisService.getUserIdByRefreshToken(oldRefreshToken);
        if (userId == null) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        CustomUserDetails userDetails = new CustomUserDetails(user);

        String newAccessToken = jwtTokenProvider.generateToken(userDetails);
        String newRefreshToken = UUID.randomUUID().toString();

        redisService.deleteRefreshToken(oldRefreshToken);
        redisService.saveRefreshToken(newRefreshToken, userId);

        UserInfoResponse userInfo = UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet()))
                .status(user.getStatus())
                .build();

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .user(userInfo)
                .build();
    }
}
