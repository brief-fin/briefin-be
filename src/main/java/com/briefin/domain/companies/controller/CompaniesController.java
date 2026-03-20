package com.briefin.domain.companies.controller;


import com.briefin.domain.companies.dto.CompanyResponseDto;
import com.briefin.domain.companies.service.CompaniesQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/companies")
public class CompaniesController {

    private final CompaniesQueryService companiesService;

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponseDto> getCompany(@PathVariable Long id) {
        return ResponseEntity.ok(companiesService.getCompany(id));
    }

    @GetMapping("/popular")
    public ResponseEntity<List<CompanyResponseDto>> getPopularCompanies() {
        return ResponseEntity.ok(companiesService.getPopularCompanies());
    }
}
