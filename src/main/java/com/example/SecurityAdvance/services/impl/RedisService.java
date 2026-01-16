package com.example.SecurityAdvance.services.impl;

import com.example.SecurityAdvance.services.IRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService implements IRedisService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String TOKEN_PREFIX = "email_verification:";
    private static final String USER_TOKENS_PREFIX = "user_tokens:";
    private static final long TOKEN_EXPIRY = 15;

    @Override
    public void saveVerificationToken(String token, Long userId) {
        String tokenKey = TOKEN_PREFIX + token;
        String userTokensKey = USER_TOKENS_PREFIX + userId;

        // Lưu token -> userId
        redisTemplate.opsForValue().set(tokenKey, userId.toString(), TOKEN_EXPIRY, TimeUnit.MINUTES);

        // Lưu vào Set của user để dễ xóa sau
        redisTemplate.opsForSet().add(userTokensKey, token);
        redisTemplate.expire(userTokensKey, TOKEN_EXPIRY, TimeUnit.SECONDS);
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
        String key = TOKEN_PREFIX + token;
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
                String tokenKey = TOKEN_PREFIX + token;
                redisTemplate.delete(tokenKey);
            }
        }

        // Xóa Set của user
        redisTemplate.delete(userTokensKey);
    }

    @Override
    public Long getTokenTTL(String token) {
        String key = TOKEN_PREFIX + token;
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }
}
