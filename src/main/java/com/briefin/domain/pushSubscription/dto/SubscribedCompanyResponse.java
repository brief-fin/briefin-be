package com.briefin.domain.pushSubscription.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SubscribedCompanyResponse {
    private Long companyId;
    private String name;
    private String ticker;
}
