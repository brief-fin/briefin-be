package com.briefin.domain.companies.controller;


import com.briefin.domain.companies.dto.CompanyResponseDto;
import com.briefin.domain.companies.service.CompaniesQueryService;
import com.briefin.domain.companies.service.CompanyDataInitService;
import com.briefin.global.apipayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/companies")
public class CompaniesController {

    private final CompaniesQueryService companiesService;
    private final CompanyDataInitService companyDataInitService;

    @GetMapping("/{id}")
    public ApiResponse<CompanyResponseDto> getCompany(@PathVariable Long id) {
        return ApiResponse.success(companiesService.getCompany(id));
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
}
