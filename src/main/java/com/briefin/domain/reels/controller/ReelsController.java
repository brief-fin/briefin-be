package com.briefin.domain.reels.controller;

import com.briefin.domain.reels.dto.ReelsResponseDTO;
import com.briefin.domain.reels.service.ReelsService;
import com.briefin.global.security.jwt.JwtUserInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Reels", description = "AI 추천 피드 API")
@RestController
@RequestMapping("/api/reels")
@RequiredArgsConstructor
public class ReelsController {

    private final ReelsService reelsService;

    @Operation(summary = "개인화 릴스 피드 조회", description = "스크랩, 조회 이력, 워치리스트 기반 벡터 유사도 추천")
    @GetMapping
    public ResponseEntity<List<ReelsResponseDTO>> getFeed(
            @AuthenticationPrincipal JwtUserInfo userInfo) {
        return ResponseEntity.ok(reelsService.getFeed(userInfo.userId()));
    }
}
