package com.briefin.domain.companies.dto;

import com.briefin.domain.companies.entity.Companies;
import com.briefin.domain.companies.entity.StockPrice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class CompanyResponseDto {

    private Long id;                        // 기업 고유 ID
    private String name;                    // 기업명
    private String ticker;                  // 종목코드
    private String sector;                  // 업종
    private String logoUrl;                 // 로고 이미지 URL
    private Double currentPrice;            // 현재가
    private Double changeRate;              // 등락률 (%)
    private Integer marketCap;
    private boolean isOverseas;
    private boolean watchlisted;
    private List<RelatedCompanyDto> relatedCompanies;  // 관련 기업 목록


    @Getter
    @Builder
    public static class RelatedCompanyDto {
        private Long id;
        private String name;
        private String ticker;
        private String logoUrl;
    }


    public static CompanyResponseDto of(Companies company, StockPrice cached) {
        return CompanyResponseDto.builder()
                .id(company.getId())
                .name(company.getName())
                .ticker(company.getTicker())
                .sector(company.getSector())
                .logoUrl(company.getLogoUrl())
                .currentPrice(cached != null ? cached.getCurrentPrice() :
                        company.getCurrentPrice() != null ? company.getCurrentPrice().doubleValue() : 0.0)
                .changeRate(cached != null ? cached.getChangeRate() :
                        company.getChangeRate() != null ? company.getChangeRate().doubleValue() : 0.0)
                .marketCap(company.getMarketCap() != null ? company.getMarketCap().intValue() : 0)
                .isOverseas(company.isOverseas())
                .build();
    }


}