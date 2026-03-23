package com.briefin.domain.companies.service;

import com.briefin.domain.companies.client.KisClient;
import com.briefin.domain.companies.entity.Companies;
import com.briefin.domain.companies.repository.CompaniesRepository;
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

    // Swagger에서 호출하는 동기화 메서드
    public void syncCompanies() {
        List<Companies> companies = companiesRepository.findAll();

        if (companies.isEmpty()) {
            log.info("동기화할 기업 데이터가 없습니다.");
            return;
        }

        log.info("기업 동기화 시작: 총 {}개", companies.size());

        for (Companies company : companies) {
            try {
                String ticker = company.getTicker();
                Map<String, Object> info = kisClient.getDomesticStockInfo(ticker);

                String sector = (String) info.get("idx_bztp_mcls_cd_name");
                log.info("섹터 조회: {} ({}) → '{}'", company.getName(), ticker, sector);

                company.setSector(sector);
                company.setLogoUrl("https://thumb.tossinvest.com/image/resized/96x0/https%3A%2F%2Fstatic.toss.im%2Fpng-icons%2Fsecurities%2Ficn-sec-fill-" + ticker + ".png");

                companiesRepository.save(company);
                log.info("동기화 완료: {} ({})", company.getName(), ticker);
                Thread.sleep(1500);

            } catch (Exception e) {
                log.error("동기화 실패: {}", company.getTicker(), e);
            }
        }

        log.info("기업 데이터 동기화 완료!");
    }
}