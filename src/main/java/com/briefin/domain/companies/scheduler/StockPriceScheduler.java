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

    @Scheduled(cron = "0 7 10 * * MON-FRI")
// @Transactional 제거 (스케줄러 자체에는 필요 없음)
    public void updateMarketCap() {
        log.info("시가총액 업데이트 시작");

        List<Companies> companies = companiesRepository.findAll();
        // 가급적 try-with-resources나 직접 shutdown 관리가 필요합니다.
        ExecutorService executor = Executors.newFixedThreadPool(3);

        for (Companies company : companies) {
            executor.submit(() -> {
                try {
                    Thread.sleep(300); // 딜레이를 조금 더 여유 있게 조정
                    StockPrice price = lsClient.getCurrentPrice(company.getTicker());

                    if (price != null && price.getMarketCap() > 0) {
                        // 개별 업데이트 로직을 별도의 트랜잭션 서비스로 분리하여 호출
                        companyUpdateService.updatePrice(company.getTicker(), price.getMarketCap());
                    }
                } catch (Exception e) {
                    log.error("시가총액 업데이트 실패: {} - {}", company.getTicker(), e.getMessage());
                }
            });
        }

        executor.shutdown();
        try {
            // 모든 작업이 완료될 때까지 최대 10분간 대기
            if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        log.info("시가총액 업데이트 완료");
    }
}