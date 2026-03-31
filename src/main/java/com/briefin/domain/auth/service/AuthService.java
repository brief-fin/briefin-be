package com.briefin.domain.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.briefin.domain.auth.dto.request.LoginRequest;
import com.briefin.domain.auth.dto.request.SignUpRequest;
import com.briefin.domain.auth.dto.result.LoginResult;
import com.briefin.domain.auth.dto.result.RefreshTokenResult;
import com.briefin.domain.auth.dto.response.SignUpResponse;
import com.briefin.domain.users.entity.Users;
import com.briefin.domain.users.repository.UsersRepository;
import com.briefin.global.apipayload.code.status.ErrorCode;
import com.briefin.global.apipayload.exception.BriefinException;
import com.briefin.global.security.jwt.JwtProvider;
import com.briefin.global.security.jwt.RefreshTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuthService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        if (usersRepository.existsByEmail(request.email())) {
            throw new BriefinException(ErrorCode.DUPLICATE_EMAIL);
        }

        if (!request.password().equals(request.passwordConfirm())) {
            throw new BriefinException(ErrorCode.PASSWORD_MISMATCH);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        Users user = Users.builder()
                .email(request.email())
                .password(encodedPassword)
                .build();

        Users savedUser = usersRepository.saveAndFlush(user);

        Users foundUser = usersRepository.findById(savedUser.getId())
                .orElseThrow(() -> new BriefinException(ErrorCode.USER_NOT_FOUND));

        return new SignUpResponse(
                foundUser.getId(),
                foundUser.getEmail(),
                foundUser.getCreatedAt()
        );
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        Users user = usersRepository.findByEmail(request.email())
                .orElseThrow(() -> new BriefinException(ErrorCode.INVALID_LOGIN));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BriefinException(ErrorCode.INVALID_LOGIN);
        }

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtProvider.createRefreshToken(user.getId(), user.getEmail());

        refreshTokenService.save(user.getId(), refreshToken);

        return LoginResult.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public RefreshTokenResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BriefinException(ErrorCode.TOKEN_MISSING);
        }
    
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BriefinException(ErrorCode.TOKEN_INVALID);
        }
    
        if (!"refresh".equals(jwtProvider.getTokenType(refreshToken))) {
            throw new BriefinException(ErrorCode.TOKEN_INVALID);
        }
    
        var userId = jwtProvider.getUserIdFromToken(refreshToken);
        var email = jwtProvider.getEmailFromToken(refreshToken);

        // 1) 동시 2회 refresh 완화:
        // - 현재 Redis 저장값과 같으면 원자적 rotate
        // - 방금 rotate된 oldRefresh라면 grace window 동안 같은 newRefresh로 멱등 처리
        // - 그 외 불일치는 재사용(탈취 가능성)으로 간주

        var savedOpt = refreshTokenService.findByUserId(userId);
        if (savedOpt.isEmpty()) {
            log.info("auth.refresh: redis miss userId={}", userId);
            throw new BriefinException(ErrorCode.REFRESH_NOT_FOUND);
        }

        String newAccessToken = jwtProvider.createAccessToken(userId, email);
        String newRefreshToken = jwtProvider.createRefreshToken(userId, email);

        var rotateResult = refreshTokenService.rotateIfMatches(userId, refreshToken, newRefreshToken);
        if (rotateResult == RefreshTokenService.RotationResult.ROTATED) {
            log.info("auth.refresh: rotated userId={}", userId);
            return RefreshTokenResult.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .build();
        }

        if (rotateResult == RefreshTokenService.RotationResult.MISMATCH) {
            // 이미 직전에 rotate된 old refresh인지 확인 (동시성/중복 호출 완화)
            var rotated = refreshTokenService.findRotatedRefresh(userId, refreshToken);
            if (rotated.isPresent()) {
                log.info("auth.refresh: idempotent (recently rotated) userId={}", userId);
                return RefreshTokenResult.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(rotated.get())
                        .build();
            }

            log.warn("auth.refresh: mismatch (possible reuse) userId={}", userId);
            refreshTokenService.delete(userId);
            throw new BriefinException(ErrorCode.TOKEN_REUSE_DETECTED);
        }

        if (rotateResult == RefreshTokenService.RotationResult.NOT_FOUND) {
            log.info("auth.refresh: redis miss during rotate userId={}", userId);
            throw new BriefinException(ErrorCode.REFRESH_NOT_FOUND);
        }

        log.error("auth.refresh: rotate error userId={}", userId);
        throw new BriefinException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BriefinException(ErrorCode.INVALID_TOKEN);
        }
    
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BriefinException(ErrorCode.INVALID_TOKEN);
        }
    
        if (!"refresh".equals(jwtProvider.getTokenType(refreshToken))) {
            throw new BriefinException(ErrorCode.INVALID_TOKEN);
        }
    
        var userId = jwtProvider.getUserIdFromToken(refreshToken);
    
        if (!refreshTokenService.matches(userId, refreshToken)) {
            throw new BriefinException(ErrorCode.INVALID_TOKEN);
        }
    
        refreshTokenService.delete(userId);
    }
}