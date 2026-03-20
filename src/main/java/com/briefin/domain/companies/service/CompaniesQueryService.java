package com.briefin.domain.companies.service;

import com.briefin.domain.companies.dto.CompanyResponseDto;
import com.briefin.domain.companies.entity.Companies;

import java.util.List;

public interface CompaniesQueryService {

    CompanyResponseDto getCompany(Long id);


    List<CompanyResponseDto> getPopularCompanies();
}
