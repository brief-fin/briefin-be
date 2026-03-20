package com.briefin.domain.users.service;

import com.briefin.domain.users.dto.UserResponseDto;
import java.util.UUID;

public interface UsersService {
    UserResponseDto getUser(UUID userId);
}

