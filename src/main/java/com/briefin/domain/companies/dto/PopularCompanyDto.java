package com.briefin.domain.companies.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PopularCompanyDto {
    private String ticker;
    private double diff;
}