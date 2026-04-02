package com.briefin.domain.companies.controller;


import com.briefin.domain.companies.dto.CompanyResponseDto;
import com.briefin.domain.companies.dto.CompanyTimelineItemDTO;
import com.briefin.domain.companies.service.CompaniesQueryService;
import com.briefin.domain.companies.service.CompanyDataInitService;
import com.briefin.domain.news.dto.NewsPageResponseDTO;
import com.briefin.domain.news.service.NewsService;
import com.briefin.global.apipayload.ApiResponse;
import com.briefin.global.security.jwt.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/companies")
public class CompaniesController {

    private final CompaniesQueryService companiesService;
    private final CompanyDataInitService companyDataInitService;
    private final NewsService newsService;

    @GetMapping("/{id}")
    @Operation(summary = "기업 상세 조회", description = "특정 기업을 상세 조회합니다.")
    @SecurityRequirement(name = "JWT TOKEN")
    public ApiResponse<CompanyResponseDto> getCompany(@PathVariable Long id) {
        UUID userId = null;
        try {
            userId = SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            // 비로그인 상태 - userId null 유지
        }
        return ApiResponse.success(companiesService.getCompany(id, userId));
    }

    @GetMapping("/popular")
    public ApiResponse<List<CompanyResponseDto>> getPopularCompanies() {
        return ApiResponse.success(companiesService.getPopularCompanies());
    }

    @GetMapping("/search")
    public ApiResponse<Page<CompanyResponseDto>> searchCompanies(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(companiesService.getSearchResultCompanies(q, pageable));
    }

    @GetMapping("/{id}/news")
    @Operation(summary = "기업 관련 뉴스 조회", description = "특정 기업과 관련된 뉴스 목록을 페이지네이션으로 반환합니다.")
    public ApiResponse<NewsPageResponseDTO> getCompanyNews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(newsService.getNewsByCompany(id, page, size));
    }

    @GetMapping("/{id}/timeline")
    @Operation(summary = "기업 타임라인 조회", description = "공시 + 기업이벤트 뉴스를 날짜순으로 반환합니다.")
    public ApiResponse<List<CompanyTimelineItemDTO>> getCompanyTimeline(@PathVariable Long id) {
        return ApiResponse.success(companiesService.getCompanyTimeline(id));
    }

    @PostMapping("/sync")
    public ApiResponse<String> syncCompanies() {
        companyDataInitService.syncCompanies();
        return ApiResponse.success("동기화 완료!");
    }
}
