package com.briefin.domain.companies.dto;

import com.briefin.domain.companies.entity.Companies;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PopularCompanyResponseDto {
    private Long id;
    private String name;
    private String ticker;
    private String sector;
    private String logoUrl;
    private double changeRate;

    public static PopularCompanyResponseDto of(Companies company, double diff) {
        String logoUrl = "https://thumb.tossinvest.com/image/resized/96x0/https%3A%2F%2Fstatic.toss.im%2Fpng-icons%2Fsecurities%2Ficn-sec-fill-" + company.getTicker() + ".png";
        return PopularCompanyResponseDto.builder()
                .id(company.getId())
                .name(company.getName())
                .ticker(company.getTicker())
                .sector(company.getSector())
                .logoUrl(logoUrl)
                .changeRate(diff)
                .build();
    }
}