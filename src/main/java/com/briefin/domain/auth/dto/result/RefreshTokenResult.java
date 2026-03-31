package com.briefin.domain.auth.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RefreshTokenResult {
    private String accessToken;
    private String refreshToken;
}

