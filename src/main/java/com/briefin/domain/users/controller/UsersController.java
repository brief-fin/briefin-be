package com.briefin.domain.users.controller;

import com.briefin.domain.users.dto.ScrapNewsResponseDto;
import com.briefin.domain.users.dto.UserResponseDto;
import com.briefin.domain.users.dto.WatchlistResponseDto;
import com.briefin.domain.users.service.ScrapsService;
import com.briefin.domain.users.service.UsersService;
import com.briefin.domain.users.service.WatchlistService;
import com.briefin.global.apipayload.ApiResponse;
import com.briefin.global.security.jwt.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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


    @PostMapping("/{id}/watch")
    @Operation(summary = "관심 기업 등록", description = "사용자가 특정 기업을 관심 등록합니다.")
    @SecurityRequirement(name = "JWT TOKEN")
    public ResponseEntity<WatchlistResponseDto.WatchlistAddResponseDto> addWatch(@PathVariable Long id) {
        return ResponseEntity.ok(watchlistService.addWatch(id, SecurityUtils.getCurrentUserId()));
    }

    @DeleteMapping("/{id}/watch")
    @Operation(summary = "관심 기업 취소", description = "사용자가 특정 기업을 관심 목록에서 취소합니다.")
    public ResponseEntity<Void> removeWatch(@PathVariable Long id) {
        watchlistService.removeWatch(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

}
