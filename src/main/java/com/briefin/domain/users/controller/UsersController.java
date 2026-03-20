package com.briefin.domain.users.controller;

import com.briefin.domain.users.dto.UserResponseDto;
import com.briefin.domain.users.service.UsersService;
import com.briefin.global.apipayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")

public class UsersController {
    private final UsersService usersService;
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> getMyInfo(
            @RequestParam UUID userId //JWT 인증 구현 전 임시
    ){
        return ResponseEntity.ok(ApiResponse.success(usersService.getUser(userId)));
    }
}
