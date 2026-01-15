package com.example.SecurityAdvance.services;

import com.example.SecurityAdvance.entities.User;

public interface IEmailService {
    void sendVerificationEmailAsync(User user, String verificationUrl);
}
