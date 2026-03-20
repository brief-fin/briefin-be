package com.briefin.domain.auth.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.briefin.global.security.jwt.JwtUserInfo;

@RestController
public class TestAuthController {

    @GetMapping("/api/test/me")
    public ResponseEntity<String> getMyInfo(Authentication authentication) {
        JwtUserInfo userInfo = (JwtUserInfo) authentication.getPrincipal();

        UUID userId = userInfo.userId();
        String email = userInfo.email();

        return ResponseEntity.ok("인증 성공 - userId: " + userId + ", email: " + email);
    }
}