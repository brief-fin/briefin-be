package com.briefin.domain.auth.controller;

import lombok.extern.slf4j.Slf4j;
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
import com.briefin.global.apipayload.ApiResponse;
import com.briefin.global.security.util.CookieUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignUpResponse>> signUp(@Valid @RequestBody SignUpRequest request) {
        SignUpResponse response = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        log.info("auth.login: email={}", request.email());
        LoginResult result = authService.login(request);

        CookieUtil.addRefreshTokenCookie(response, result.getRefreshToken());
        log.info("auth.login: refresh cookie issued (token omitted)");

        LoginResponse body = LoginResponse.builder()
                .accessToken(result.getAccessToken())
                .build();
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = CookieUtil.getRefreshTokenFromCookie(request);
        log.info("auth.refresh: entered, cookiePresent={}", refreshToken != null && !refreshToken.isBlank());

        RefreshTokenResult result = authService.refresh(refreshToken);

        CookieUtil.addRefreshTokenCookie(response, result.getRefreshToken());
        log.info("auth.refresh: rotation success, refresh cookie updated (token omitted)");

        RefreshTokenResponse body = RefreshTokenResponse.builder()
                .accessToken(result.getAccessToken())
                .build();
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = CookieUtil.getRefreshTokenFromCookie(request);
        log.info("auth.logout: entered, cookiePresent={}", refreshToken != null && !refreshToken.isBlank());

        authService.logout(refreshToken);
        CookieUtil.deleteRefreshTokenCookie(response);
        log.info("auth.logout: redis deleted + cookie expired (token omitted)");

        return ResponseEntity.ok(ApiResponse.<Void>success(null));
    }
}