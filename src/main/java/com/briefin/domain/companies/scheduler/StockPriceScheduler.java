package com.briefin.domain.companies.scheduler;

import com.briefin.domain.companies.repository.CompaniesRepository;
import com.briefin.domain.companies.socket.StockPriceCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockPriceScheduler {

    private final StockPriceCache stockPriceCache;
    private final CompaniesRepository companiesRepository;

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
}