package com.example.SecurityAdvance.services;

public interface IRedisService {
    void saveVerificationToken(String token, Long userId);
    Long getUserIdByToken(String token);
    void deleteToken(String token);
    boolean isTokenValid(String token);
    void deleteAllUserTokens(Long userId);
    Long getTokenTTL(String token);
}
