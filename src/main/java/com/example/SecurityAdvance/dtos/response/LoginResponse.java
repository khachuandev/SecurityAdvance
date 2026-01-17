package com.example.SecurityAdvance.dtos.response;

import lombok.*;

@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private UserInfoResponse user;
}
