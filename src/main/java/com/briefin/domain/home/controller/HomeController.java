package com.briefin.domain.home.controller;

import com.briefin.domain.news.dto.HomeNewsResponseDTO;
import com.briefin.domain.news.service.NewsService;
import com.briefin.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
@Tag(name = "Home", description = "홈 API")
public class HomeController {

    private final NewsService newsService;

    @Operation(summary = "홈 뉴스 조회", description = "국내/해외 최신 뉴스 각 3개 반환")
    @GetMapping
    public ApiResponse<HomeNewsResponseDTO> getHomeNews() {
        return ApiResponse.success(newsService.getHomeNews());
    }
}
