package com.briefin.domain.companies.scheduler;

import com.briefin.domain.companies.client.LsClient;
import com.briefin.domain.companies.entity.Companies;
import com.briefin.domain.companies.entity.StockPrice;
import com.briefin.domain.companies.repository.CompaniesRepository;
import com.briefin.domain.companies.service.CompanyUpdateService;
import com.briefin.domain.companies.socket.StockPriceCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockPriceScheduler {

    private final StockPriceCache stockPriceCache;
    private final CompaniesRepository companiesRepository;
    private final LsClient lsClient;
    private final CompanyUpdateService companyUpdateService;

    // 장 마감 후 15:35 DB에 종가 저장
    @Scheduled(cron = "0 35 15 * * MON-FRI")
    @Transactional
    public void saveClosingPrice() {
        log.info("장 마감 - 종가 DB 저장 시작");

        stockPriceCache.getAll().forEach((ticker, price) ->
                companiesRepository.updatePrice(
                        ticker,
                        BigDecimal.valueOf(price.getCurrentPrice()),
                        BigDecimal.valueOf(price.getChangeRate())
                )
        );

        log.info("장 마감 - 종가 DB 저장 완료");
    }

    @Scheduled(cron = "0 37 11 * * MON-FRI")
    public void updateMarketCap() {
        log.info("시가총액 업데이트 시작 (안정성 모드)");

        // 1. 모든 기업 리스트 조회
        List<Companies> companies = companiesRepository.findAll();

        for (Companies company : companies) {
            try {
                // 2. API 호출 전 충분한 간격 부여 (초당 2건 이하로 제한)
                // LS증권 API의 초당 호출 제한을 피하기 위해 600ms~1s 정도 권장
                Thread.sleep(600);

                // 3. API 호출
                StockPrice price = lsClient.getCurrentPrice(company.getTicker());

                // 4. 엄격한 검증 로직 (0값 및 null 방지)
                if (price != null && price.getMarketCap() > 0) {
                    // 성공 시 업데이트
                    companyUpdateService.updatePrice(company.getTicker(), price.getMarketCap());
                    log.info("성공: {} ({}억)", company.getTicker(), price.getMarketCap());
                } else {
                    // 데이터가 0이거나 null인 경우 로그만 남기고 업데이트 스킵 (기존 값 보존)
                    log.warn("건너뜀: {} (데이터가 0이거나 부적절함)", company.getTicker());
                }

            } catch (InterruptedException e) {
                log.error("스케줄러 작업 중단됨");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("시가총액 업데이트 오류 발생: {} - {}", company.getTicker(), e.getMessage());
            }
        }

        log.info("시가총액 업데이트 완료");
    }
}