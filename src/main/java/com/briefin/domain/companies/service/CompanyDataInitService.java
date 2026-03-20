package com.briefin.domain.companies.service;

import com.briefin.domain.companies.client.KisClient;
import com.briefin.domain.companies.entity.Companies;
import com.briefin.domain.companies.repository.CompaniesRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyDataInitService {

    private final CompaniesRepository companiesRepository;
    private final KisClient kisClient;

    private static final List<String> INITIAL_TICKERS = List.of(
            "005930",  // 삼성전자
            "000660",  // SK하이닉스
            "214420",  // 올리브네트웍스
            "005380",  // 현대차
            "035720",  // 카카오
            "035420",  // NAVER
            "373220",  // LG에너지솔루션
            "068270"   // 셀트리온
    );

    @PostConstruct
    public void initCompanies() {
        if (companiesRepository.count() > 0) {
            log.info("이미 데이터가 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        kisClient.getAccessToken();

        for (String ticker : INITIAL_TICKERS) {
            try {
                Map<String, Object> info = kisClient.getDomesticStockInfo(ticker);

                Companies company = Companies.builder()
                        .ticker(ticker)
                        .name((String) info.get("prdt_abrv_name"))
                        .sector((String) info.get("idx_bztp_mcls_cd_name"))
                        .logoUrl("https://ssl.pstatic.net/imgfinance/company/mobile/" + ticker + ".png")
                        .isOverseas(false)
                        .build();

                companiesRepository.save(company);
                log.info("저장 완료: {} ({})", company.getName(), ticker);
                Thread.sleep(1500);

            } catch (Exception e) {
                log.error("저장 실패: {}", ticker, e);
            }
        }

        log.info("기업 데이터 초기화 완료!");
    }
}