package com.briefin.global.security.jwt;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    public void save(UUID userId, String refreshToken) {
        String key = generateKey(userId);
        redisTemplate.opsForValue().set(
                key,
                refreshToken,
                Duration.ofMillis(refreshTokenExpiration)
        );
    }

    public Optional<String> findByUserId(UUID userId) {
        String key = generateKey(userId);
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    public boolean exists(UUID userId) {
        String key = generateKey(userId);
        Boolean result = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(result);
    }

    public void delete(UUID userId) {
        String key = generateKey(userId);
        redisTemplate.delete(key);
    }

    public boolean matches(UUID userId, String refreshToken) {
        return findByUserId(userId)
                .map(savedToken -> savedToken.equals(refreshToken))
                .orElse(false);
    }

    private String generateKey(UUID userId) {
        return REFRESH_TOKEN_PREFIX + userId;
    }
}