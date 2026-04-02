package com.briefin.domain.companies.service;

import com.briefin.domain.companies.dto.CompanyResponseDto;
import com.briefin.domain.companies.dto.CompanyTimelineItemDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CompaniesQueryService {

    CompanyResponseDto getCompany(Long id, UUID userId);

    List<CompanyResponseDto> getPopularCompanies();

    Page<CompanyResponseDto> getSearchResultCompanies(String name, Pageable pageable);

    List<CompanyTimelineItemDTO> getCompanyTimeline(Long companyId);
}
