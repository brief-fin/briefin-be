package com.briefin.domain.auth.dto.response;

import java.util.UUID;
import java.time.LocalDateTime;

public record SignUpResponse (
    UUID id,
    String email,
    LocalDateTime createdAt
){
}
