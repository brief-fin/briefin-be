package com.briefin.domain.companies.service;

import com.briefin.domain.companies.dto.CompanyResponseDto;
import com.briefin.domain.companies.entity.Companies;

public interface CompaniesQueryService {

    CompanyResponseDto getCompany(Long id);
}
