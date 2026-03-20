package com.briefin.domain.users.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder

public class UserResponseDto {
    private UUID userId;
    private String email;
    private LocalDateTime createdAt;
}
