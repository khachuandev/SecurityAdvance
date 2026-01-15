package com.example.SecurityAdvance.services.impl;

import com.example.SecurityAdvance.dtos.request.UserRegisterRequest;
import com.example.SecurityAdvance.dtos.response.UserResponse;
import com.example.SecurityAdvance.entities.EncryptionKey;
import com.example.SecurityAdvance.entities.User;
import com.example.SecurityAdvance.enums.EncryptionKeyStatus;
import com.example.SecurityAdvance.enums.UserStatus;
import com.example.SecurityAdvance.repositories.EncryptionKeyRepository;
import com.example.SecurityAdvance.repositories.UserRepository;
import com.example.SecurityAdvance.services.IAuthService;
import com.example.SecurityAdvance.utils.AESUtils;
import com.example.SecurityAdvance.utils.HashUtils;
import com.example.SecurityAdvance.utils.RSAUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {
    private final UserRepository userRepository;
    private final EncryptionKeyRepository encryptionKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final HashUtils hashUtils;
    private final AESUtils aesUtils;
    private final RSAUtils rsaUtils;

    @Override
    public UserResponse register(UserRegisterRequest request) {
        if (!request.getPassword().equals(request.getRetypePassword())) {
            throw new IllegalArgumentException("Password không khớp");
        }

        // Check email tồn tại (plain text)
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email đã tồn tại");
        }

        /* ========== 1. GEN KEYS ========== */
        SecretKey dek = aesUtils.generateAESKey(); // Data Encryption Key
        SecretKey kek = aesUtils.generateAESKey(); // Key Encryption Key
        byte[] dataIv = aesUtils.generateIV();
        byte[] keyIv = aesUtils.generateIV();

        /* ========== 2. ENCRYPT USER DATA ========== */
        String encryptedPhone = aesUtils.encrypt(request.getPhoneNumber(), dek, dataIv);
        String phoneHash = hashUtils.sha256(request.getPhoneNumber());

        /* ========== 3. WRAP DEK (AES) ========== */
        String dekBase64 = aesUtils.encodeKeyBase64(dek);
        String encryptedDek = aesUtils.encrypt(dekBase64, kek, keyIv);

        /* ========== 4. WRAP KEK (RSA) ========== */
        String kekBase64 = aesUtils.encodeKeyBase64(kek);
        String encryptedKek = rsaUtils.encryptAESKey(kekBase64);

        /* ========== 5. SAVE USER ========== */
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .phoneNumber(encryptedPhone)
                .phoneHash(phoneHash)
                .dataIv(Base64.getEncoder().encodeToString(dataIv))
                .status(UserStatus.PENDING_VERIFICATION)
                .build();

        userRepository.save(user);

        /* ========== 6. SAVE ENCRYPTION KEY ========== */
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
}
