package com.briefin.domain.reels.controller;

import com.briefin.domain.reels.dto.ReelsResponseDTO;
import com.briefin.domain.reels.service.ReelsService;
import com.briefin.global.security.jwt.JwtUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reels")
@RequiredArgsConstructor
public class ReelsController {

    private final ReelsService reelsService;

    @GetMapping
    public ResponseEntity<List<ReelsResponseDTO>> getFeed(
            @AuthenticationPrincipal JwtUserInfo userInfo) {
        return ResponseEntity.ok(reelsService.getFeed(userInfo.userId()));
    }
}
