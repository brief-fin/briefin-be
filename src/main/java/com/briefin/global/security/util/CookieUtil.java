package com.briefin.global.security.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseCookie;

public class CookieUtil {

    private CookieUtil() {
    }

    public static void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        // NOTE:
        // - SameSite 기본값은 브라우저마다 달라 예측이 어렵다.
        // - Next.js(localhost:3000) -> API(localhost:8080) 호출에서 쿠키가 빠지는 이슈를 줄이기 위해 명시한다.
        // - 개발(localhost)에서는 Secure=false + SameSite=Lax로 충분(동일 site). HTTPS 운영에선 Secure=true + SameSite=None 권장.
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(14 * 24 * 60 * 60)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public static void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public static String getRefreshTokenFromCookie(HttpServletRequest request) {
        // Servlet Cookie 기반 조회가 가장 안전하지만, SameSite/속성 이슈로 헤더에만 존재하는 경우도 있어 fallback을 둔다.
        var cookies = request.getCookies();
        if (cookies != null) {
            for (var cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // Fallback: Cookie 헤더 직접 파싱 (필요 시에만)
        String cookieHeader = request.getHeader("Cookie");
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return null;
        }
        String[] parts = cookieHeader.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.startsWith("refreshToken=")) {
                return trimmed.substring("refreshToken=".length());
            }
        }
        return null;
    }
}