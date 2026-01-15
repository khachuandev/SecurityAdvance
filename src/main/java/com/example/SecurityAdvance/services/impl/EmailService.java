package com.example.SecurityAdvance.services.impl;

import com.example.SecurityAdvance.entities.User;
import com.example.SecurityAdvance.services.IEmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {
    private final JavaMailSender mailSender;

    @Async
    @Override
    public void sendVerificationEmailAsync(User user, String verificationUrl) {
        try {
            sendVerificationEmail(user, verificationUrl);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Error sending verification email to {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    private void sendVerificationEmail(User user, String verificationUrl)
            throws MessagingException, UnsupportedEncodingException {

        String subject = "Email Verification - Action Required";
        String senderName = "SecurityAdvance Service";

        String mailContent = "<div style='font-family: Arial, sans-serif;'>" +
                "<h2>Email Verification</h2>" +
                "<p>Hi <strong>" + user.getUsername() + "</strong>,</p>" +
                "<p>Thank you for registering with us. Please click the button below to verify your email:</p>" +
                "<div style='margin: 30px 0;'>" +
                "<a href=\"" + verificationUrl + "\" " +
                "style='background-color: #4CAF50; color: white; padding: 14px 20px; " +
                "text-decoration: none; border-radius: 4px; display: inline-block;'>" +
                "Verify Email" +
                "</a>" +
                "</div>" +
                "<p><strong>This link will expire in 15 minutes.</strong></p>" +
                "<p>If the button doesn't work, copy and paste this link:</p>" +
                "<p style='background-color: #f5f5f5; padding: 10px; word-break: break-all;'>" +
                verificationUrl +
                "</p>" +
                "<p>If you didn't create an account, please ignore this email.</p>" +
                "<hr style='margin: 30px 0;'>" +
                "<p style='color: #666; font-size: 12px;'>Thank you,<br>SecurityAdvance Team</p>" +
                "</div>";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom("huan12a2002@gmail.com", senderName);
        helper.setTo(user.getEmail());
        helper.setSubject(subject);
        helper.setText(mailContent, true);

        long startTime = System.currentTimeMillis();
        mailSender.send(message);
        long endTime = System.currentTimeMillis();

        log.info("Verification email sent to {} in {} ms", user.getEmail(), (endTime - startTime));
    }
}
