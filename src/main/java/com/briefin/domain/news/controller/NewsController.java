package com.briefin.domain.news.controller;

import com.briefin.domain.news.dto.*;
import com.briefin.domain.news.service.NewsService;
import com.briefin.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Tag(name = "News", description = "뉴스 API")
public class NewsController {

    private final NewsService newsService;

    @Operation(summary = "뉴스 목록 조회", description = "category 미전달 시 전체 반환")
    @GetMapping
    public ApiResponse<List<NewsListResponseDTO>> getNewsList(
            @RequestParam(required = false, defaultValue = "all") String category) {
        return ApiResponse.success(newsService.getNewsList(category));
    }

    @Operation(summary = "뉴스 상세 조회")
    @GetMapping("/{id}")
    public ApiResponse<NewsDetailResponseDTO> getNewsDetail(@PathVariable Long id) {
        return ApiResponse.success(newsService.getNewsDetail(id));
    }

    @Operation(summary = "키워드로 뉴스 검색")
    @GetMapping("/search")
    public ApiResponse<List<NewsSearchResponseDTO>> searchNews(@RequestParam String q) {
        return ApiResponse.success(newsService.searchNews(q));
    }

    @Operation(summary = "관련 뉴스 목록 조회")
    @GetMapping("/{id}/related")
    public ApiResponse<List<NewsRelatedResponseDTO>> getRelatedNews(@PathVariable Long id) {
        return ApiResponse.success(newsService.getRelatedNews(id));
    }
}
