package com.example.SecurityAdvance.services;

import com.example.SecurityAdvance.dtos.request.UserLoginDto;
import com.example.SecurityAdvance.dtos.request.UserRegisterRequest;
import com.example.SecurityAdvance.dtos.response.LoginResponse;
import com.example.SecurityAdvance.dtos.response.UserResponse;

public interface IAuthService {
    UserResponse register(UserRegisterRequest request);
    void verifyEmail(String token);
    void resendVerificationEmail(String email, String applicationUrl);
    LoginResponse login(UserLoginDto request);
}
