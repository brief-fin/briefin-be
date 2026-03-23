package com.briefin.domain.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.briefin.domain.auth.dto.request.LoginRequest;
import com.briefin.domain.auth.dto.request.SignUpRequest;
import com.briefin.domain.auth.dto.response.LoginResponse;
import com.briefin.domain.auth.dto.response.RefreshTokenResponse;
import com.briefin.domain.auth.dto.response.SignUpResponse;
import com.briefin.domain.auth.dto.result.LoginResult;
import com.briefin.domain.auth.dto.result.RefreshTokenResult;
import com.briefin.domain.auth.service.AuthService;
import com.briefin.global.security.util.CookieUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        SignUpResponse response = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        LoginResult result = authService.login(request);

        CookieUtil.addRefreshTokenCookie(response, result.getRefreshToken());

        return ResponseEntity.ok(
                LoginResponse.builder()
                        .accessToken(result.getAccessToken())
                        .build()
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = CookieUtil.getRefreshTokenFromCookie(request);

        RefreshTokenResult result = authService.refresh(refreshToken);

        CookieUtil.addRefreshTokenCookie(response, result.getRefreshToken());

        return ResponseEntity.ok(
                RefreshTokenResponse.builder()
                        .accessToken(result.getAccessToken())
                        .build()
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = CookieUtil.getRefreshTokenFromCookie(request);

        authService.logout(refreshToken);
        CookieUtil.deleteRefreshTokenCookie(response);

        return ResponseEntity.ok().build();
    }
}