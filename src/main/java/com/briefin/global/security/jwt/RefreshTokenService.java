package com.briefin.global.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String ROTATION_MAP_PREFIX = "refresh-rotated:";

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * refresh rotation 동시성 완화를 위한 grace window (ms).
     * 거의 동시에 같은 refresh로 2번 요청이 들어오면, 두 번째는 "재사용" 대신 동일한 새 refresh로 처리할 수 있게 한다.
     */
    @Value("${jwt.refresh-rotation-grace-ms:5000}")
    private long rotationGraceMs;

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

    /**
     * 원자적으로 refresh를 회전한다.
     * - 현재 Redis 저장값이 presented와 같을 때만 newToken으로 교체
     * - 동시에 old->new 매핑을 짧게 저장해(그레이스) 거의 동시 2회 요청을 멱등 처리할 수 있게 한다.
     *
     * @return true면 회전 성공(현재값이 presented와 일치), false면 불일치 또는 없음
     */
    public RotationResult rotateIfMatches(UUID userId, String presented, String newToken) {
        String key = generateKey(userId);
        String mapKey = generateRotationMapKey(userId, presented);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                local cur = redis.call('GET', KEYS[1])
                if (not cur) then
                  return 0
                end
                if (cur ~= ARGV[1]) then
                  return -1
                end
                redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
                redis.call('SET', KEYS[2], ARGV[2], 'PX', ARGV[4])
                return 1
                """);

        Long result = redisTemplate.execute(
                script,
                java.util.List.of(key, mapKey),
                presented,
                newToken,
                String.valueOf(refreshTokenExpiration),
                String.valueOf(rotationGraceMs)
        );

        if (result == null) return RotationResult.ERROR;
        if (result == 1L) return RotationResult.ROTATED;
        if (result == 0L) return RotationResult.NOT_FOUND;
        if (result == -1L) return RotationResult.MISMATCH;
        return RotationResult.ERROR;
    }

    /**
     * 이전 refresh로 이미 회전된 새 refresh를 grace window 동안 조회한다.
     * (동시 2회 refresh 요청이 들어온 경우 두 번째 요청을 멱등 처리하기 위함)
     */
    public Optional<String> findRotatedRefresh(UUID userId, String oldRefreshToken) {
        String mapKey = generateRotationMapKey(userId, oldRefreshToken);
        return Optional.ofNullable(redisTemplate.opsForValue().get(mapKey));
    }

    private String generateKey(UUID userId) {
        return REFRESH_TOKEN_PREFIX + userId;
    }

    private String generateRotationMapKey(UUID userId, String oldRefreshToken) {
        // 토큰 원문을 key로 쓰지 않기 위해 해시
        return ROTATION_MAP_PREFIX + userId + ":" + sha256Hex(oldRefreshToken);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 JDK에 기본 포함
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public enum RotationResult {
        ROTATED,
        NOT_FOUND,
        MISMATCH,
        ERROR
    }
}