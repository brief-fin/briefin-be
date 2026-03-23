package com.briefin.domain.users.controller;

import com.briefin.domain.users.dto.ScrapNewsResponseDto;
import com.briefin.domain.users.dto.UserResponseDto;
import com.briefin.domain.users.dto.WatchlistResponseDto;
import com.briefin.domain.users.service.ScrapsService;
import com.briefin.domain.users.service.UsersService;
import com.briefin.domain.users.service.WatchlistService;
import com.briefin.global.apipayload.ApiResponse;
import com.briefin.global.security.jwt.JwtUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")

public class UsersController {
    private final UsersService usersService;
    private final ScrapsService scrapsService;
    private final WatchlistService watchlistService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> getMyInfo(
            @AuthenticationPrincipal JwtUserInfo jwtUserInfo
    ){
        return ResponseEntity.ok(ApiResponse.success(usersService.getUser(jwtUserInfo.userId())));
    }

    @GetMapping("/scraps")
    public ResponseEntity<ApiResponse<ScrapNewsResponseDto>> getScrappedNews(
            @AuthenticationPrincipal JwtUserInfo jwtUserInfo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(scrapsService.getScrappedNews(jwtUserInfo.userId(), page, size)));
    }

    @GetMapping("/watchlist")
    public ResponseEntity<ApiResponse<WatchlistResponseDto>> getWatchlist(
            @AuthenticationPrincipal JwtUserInfo jwtUserInfo
    ) {
        return ResponseEntity.ok(ApiResponse.success(watchlistService.getWatchlist(jwtUserInfo.userId())));
    }
}
