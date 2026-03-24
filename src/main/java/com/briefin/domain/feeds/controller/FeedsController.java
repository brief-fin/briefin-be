package com.briefin.domain.feeds.controller;

import com.briefin.domain.feeds.dto.FeedsResponseDTO;
import com.briefin.domain.feeds.service.FeedsService;
import com.briefin.global.security.jwt.JwtUserInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Feeds", description = "워치리스트 기반 피드 API")
@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedsController {

    private final FeedsService feedsService;

    @Operation(summary = "워치리스트 피드 조회", description = "관심 등록한 기업들의 뉴스를 최신순으로 반환")
    @GetMapping
    public ResponseEntity<List<FeedsResponseDTO>> getFeed(
            @AuthenticationPrincipal JwtUserInfo userInfo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(feedsService.getFeed(userInfo.userId(), page, size));
    }
}
