package com.example.SecurityAdvance.services.impl;

import com.example.SecurityAdvance.services.IRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService implements IRedisService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String TOKEN_PREFIX = "email_verification:";
    private static final long TOKEN_EXPIRY = 15;

    @Override
    public void saveVerificationToken(String token, Long userId) {
        String key = TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(key, userId.toString(), TOKEN_EXPIRY, TimeUnit.MINUTES);
    }

    @Override
    public Long getUserIdByToken(String token) {
        String key = TOKEN_PREFIX + token;
        String userId = redisTemplate.opsForValue().get(key);
        return userId != null ? Long.parseLong(userId) : null;
    }

    @Override
    public void deleteToken(String token) {
        String key = TOKEN_PREFIX + token;
        redisTemplate.delete(key);
    }

    @Override
    public boolean isTokenValid(String token) {
        String key = TOKEN_PREFIX + token;
        return redisTemplate.hasKey(key);
    }

    @Override
    public void deleteAllUserTokens(Long userId) {
        String pattern = TOKEN_PREFIX + "*";
        redisTemplate.keys(pattern).forEach(key -> {
            String value = redisTemplate.opsForValue().get(key);
            if (value != null && value.equals(userId.toString())) {
                redisTemplate.delete(key);
            }
        });
    }

    @Override
    public Long getTokenTTL(String token) {
        String key = TOKEN_PREFIX + token;
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }
}
