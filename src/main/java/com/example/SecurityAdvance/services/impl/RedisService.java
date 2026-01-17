package com.example.SecurityAdvance.services.impl;

import com.example.SecurityAdvance.services.IRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService implements IRedisService {
    @Value("${jwt.expiration-refresh-token}")
    private Long expirationRefreshToken;

    private final RedisTemplate<String, String> redisTemplate;

    private static final String EMAIL_TOKEN_PREFIX = "email_verification:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String USER_TOKENS_PREFIX = "user_tokens:";
    private static final String USER_REFRESH_PREFIX = "user_refresh:";
    private static final long EMAIL_TOKEN_EXPIRY = 15;


    @Override
    public void saveVerificationToken(String token, Long userId) {
        String tokenKey = EMAIL_TOKEN_PREFIX + token;
        String userTokensKey = USER_TOKENS_PREFIX + userId;

        // Lưu token -> userId
        redisTemplate.opsForValue().set(tokenKey, userId.toString(), EMAIL_TOKEN_EXPIRY, TimeUnit.MINUTES);

        // Lưu vào Set của user để dễ xóa sau
        redisTemplate.opsForSet().add(userTokensKey, token);
        redisTemplate.expire(userTokensKey, EMAIL_TOKEN_EXPIRY, TimeUnit.MINUTES);
    }

    @Override
    public Long getUserIdByToken(String token) {
        String key = EMAIL_TOKEN_PREFIX + token;
        String userId = redisTemplate.opsForValue().get(key);
        return userId != null ? Long.parseLong(userId) : null;
    }

    @Override
    public void deleteToken(String token) {
        String key = EMAIL_TOKEN_PREFIX + token;
        Long userId = getUserIdByToken(token);

        redisTemplate.delete(key);

        // Xóa khỏi Set của user
        if (userId != null) {
            String userTokensKey = USER_TOKENS_PREFIX + userId;
            redisTemplate.opsForSet().remove(userTokensKey, token);
        }
    }

    @Override
    public boolean isTokenValid(String token) {
        String key = EMAIL_TOKEN_PREFIX + token;
        return redisTemplate.hasKey(key);
    }

    @Override
    public void deleteAllUserTokens(Long userId) {
        String userTokensKey = USER_TOKENS_PREFIX + userId;

        // Lấy tất cả tokens của user
        Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);

        if (tokens != null && !tokens.isEmpty()) {
            // Xóa từng token
            for (String token : tokens) {
                String tokenKey = EMAIL_TOKEN_PREFIX + token;
                redisTemplate.delete(tokenKey);
            }
        }

        // Xóa Set của user
        redisTemplate.delete(userTokensKey);
    }

    @Override
    public Long getTokenTTL(String token) {
        String key = EMAIL_TOKEN_PREFIX + token;
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    @Override
    public void saveRefreshToken(String refreshToken, Long userId) {
        long ttlSeconds = expirationRefreshToken / 1000;

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + refreshToken,
                userId.toString(),
                ttlSeconds,
                TimeUnit.SECONDS);

        redisTemplate.opsForValue()
                .set(USER_REFRESH_PREFIX + userId,
                        refreshToken,
                        ttlSeconds,
                        TimeUnit.SECONDS);
    }

    @Override
    public Long getUserIdByRefreshToken(String refreshToken) {
        String val = redisTemplate.opsForValue()
                .get(REFRESH_TOKEN_PREFIX + refreshToken);
        return val != null ? Long.parseLong(val) : null;
    }

    @Override
    public void deleteRefreshToken(String refreshToken) {
        Long userId = getUserIdByRefreshToken(refreshToken);
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + refreshToken);

        if (userId != null) {
            redisTemplate.delete(USER_REFRESH_PREFIX + userId);
        }
    }
}
