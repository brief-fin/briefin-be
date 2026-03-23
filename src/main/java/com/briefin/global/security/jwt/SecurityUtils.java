package com.briefin.global.security.jwt;

import org.springframework.security.core.context.SecurityContextHolder;
import java.util.UUID;

public class SecurityUtils {

    public static UUID getCurrentUserId() {
        JwtUserInfo jwtUserInfo = (JwtUserInfo) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        return jwtUserInfo.userId();
    }
}