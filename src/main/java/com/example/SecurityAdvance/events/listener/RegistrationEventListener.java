package com.example.SecurityAdvance.events.listener;

import com.example.SecurityAdvance.entities.User;
import com.example.SecurityAdvance.events.RegistrationEvent;
import com.example.SecurityAdvance.services.IEmailService;
import com.example.SecurityAdvance.services.IRedisService;
import com.example.SecurityAdvance.services.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationEventListener {
    @Value("${api.prefix}")
    private String apiPrefix;

    private final IUserService userService;
    private final IRedisService redisService;
    private final IEmailService emailService;

    @EventListener
    public void handleRegistrationEvent(RegistrationEvent event) {
        User user = userService.findById(event.userId());

        // Xoá token cũ nếu có (tránh spam)
        redisService.deleteAllUserTokens(user.getId());

        // Tạo token mới
        String verificationToken = UUID.randomUUID().toString();
        redisService.saveVerificationToken(verificationToken, user.getId());

        String verificationUrl = event.applicationUrl() + apiPrefix +
                "/auth/verify-email?token=" + verificationToken;

        // Gửi email
        emailService.sendVerificationEmailAsync(user, verificationUrl);
        log.info("Verification email process initiated for user: {}", user.getUsername());
    }
}
