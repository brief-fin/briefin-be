package com.briefin.domain.news.controller;

import com.briefin.domain.news.dto.*;
import com.briefin.domain.news.service.NewsService;
import com.briefin.domain.users.service.ScrapsService;
import com.briefin.global.apipayload.ApiResponse;
import com.briefin.global.security.jwt.JwtUserInfo;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Tag(name = "News", description = "뉴스 API")
public class NewsController {

    private final NewsService newsService;
    private final ScrapsService scrapsService;

    @Operation(summary = "뉴스 목록 조회", description = "category 미전달 시 전체 반환. 무한스크롤용 페이지네이션 지원")
    @GetMapping
    public ApiResponse<NewsPageResponseDTO> getNewsList(
            @RequestParam(required = false, defaultValue = "all") String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(newsService.getNewsList(category, page, size));
    }

    @Operation(summary = "뉴스 상세 조회")
    @GetMapping("/{id}")
    public ApiResponse<NewsDetailResponseDTO> getNewsDetail(
            @AuthenticationPrincipal JwtUserInfo userInfo,
            @PathVariable Long id) {
        return ApiResponse.success(newsService.getNewsDetail(id, userInfo.userId()));
    }

    @Operation(summary = "키워드로 뉴스 검색")
    @GetMapping("/search")
    public ApiResponse<NewsPageResponseDTO> searchNews(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(newsService.searchNews(q, page, size));
    }

    @Operation(summary = "관련 뉴스 목록 조회")
    @GetMapping("/{id}/related")
    public ApiResponse<List<NewsRelatedResponseDTO>> getRelatedNews(@PathVariable Long id) {
        return ApiResponse.success(newsService.getRelatedNews(id));
    }

    @Operation(summary = "뉴스 타임라인 조회", description = "해당 뉴스와 같은 기업의 관련 뉴스를 시간순으로 반환. isCurrent=true가 현재 기사")
    @GetMapping("/{id}/timeline")
    public ApiResponse<List<NewsTimelineItemDTO>> getNewsTimeline(@PathVariable Long id) {
        return ApiResponse.success(newsService.getNewsTimeline(id));
    }

    @Operation(summary = "뉴스 경제 용어 해설 조회", description = "기사 본문에 등장하는 어려운 경제 용어와 설명을 반환")
    @GetMapping("/{id}/terms")
    public ApiResponse<List<TermExplanationDTO>> getTermExplanations(@PathVariable Long id) {
        return ApiResponse.success(newsService.getTermExplanations(id));
    }

    @Operation(summary = "뉴스 스크랩 등록")
    @PostMapping("/{id}/scrap")
    public ApiResponse<ScrapResponseDto> addScrap(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserInfo jwtUserInfo
    ) {
        return ApiResponse.success(scrapsService.addScrap(jwtUserInfo.userId(), id));
    }

    @Operation(summary = "뉴스 스크랩 취소")
    @DeleteMapping("/{id}/scrap")
    public ApiResponse<ScrapResponseDto> removeScrap(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserInfo jwtUserInfo
    ) {
        return ApiResponse.success(scrapsService.removeScrap(jwtUserInfo.userId(), id));
    }
}
