package com.briefin.domain.companies.controller;


import com.briefin.domain.companies.dto.CompanyResponseDto;
import com.briefin.domain.companies.service.CompaniesQueryService;
import com.briefin.domain.companies.service.CompanyDataInitService;
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
    public ResponseEntity<CompanyResponseDto> getCompany(@PathVariable Long id) {
        return ResponseEntity.ok(companiesService.getCompany(id));
    }

    @GetMapping("/popular")
    public ResponseEntity<List<CompanyResponseDto>> getPopularCompanies() {
        return ResponseEntity.ok(companiesService.getPopularCompanies());
    }

    @PostMapping("/sync")
    public ResponseEntity<String> syncCompanies() {
        companyDataInitService.syncCompanies();
        return ResponseEntity.ok("동기화 완료!");
    }

    @GetMapping("/search")
    public ResponseEntity<Page<CompanyResponseDto>> searchCompanies(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(companiesService.getSearchResultCompanies(q, pageable));
    }
}
