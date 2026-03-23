package com.briefin.global.security.jwt;

import java.util.UUID;

public record JwtUserInfo (
    UUID userId,
    String email
) {}
