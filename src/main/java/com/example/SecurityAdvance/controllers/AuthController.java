package com.example.SecurityAdvance.controllers;

import com.example.SecurityAdvance.dtos.request.UserLoginDto;
import com.example.SecurityAdvance.dtos.request.UserRegisterRequest;
import com.example.SecurityAdvance.dtos.response.ApiRes;
import com.example.SecurityAdvance.dtos.response.LoginResponse;
import com.example.SecurityAdvance.dtos.response.UserResponse;
import com.example.SecurityAdvance.events.RegistrationEvent;
import com.example.SecurityAdvance.services.IAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/auth")
public class AuthController {
    private final IAuthService authService;
    private final ApplicationEventPublisher eventPublisher;

    @PostMapping("/register")
    public ResponseEntity<ApiRes<UserResponse>> register(@Valid @RequestBody UserRegisterRequest request,
                                           HttpServletRequest httpRequest) {
        UserResponse newUser = authService.register(request);
        String appUrl = getApplicationUrl(httpRequest);
        eventPublisher.publishEvent(new RegistrationEvent(newUser.getId(), appUrl));
        return ResponseEntity.ok(ApiRes.created(newUser));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiRes<LoginResponse>> login(@Valid @RequestBody UserLoginDto request){
        LoginResponse login = authService.login(request);
        return ResponseEntity.ok(ApiRes.success(login));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiRes<String>> verifyEmail(@RequestParam("token") String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiRes.success("Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiRes<String>> resendEmail(@RequestParam String email, HttpServletRequest httpRequest) {
        String appUrl = getApplicationUrl(httpRequest);
        authService.resendVerificationEmail(email, appUrl);
        return ResponseEntity.ok(ApiRes.success("Verification email has been resent. Please check your inbox."));
    }

    private String getApplicationUrl(HttpServletRequest request) {
        return request.getScheme() + "://" +
                request.getServerName() + ":" +
                request.getServerPort();
    }
}
