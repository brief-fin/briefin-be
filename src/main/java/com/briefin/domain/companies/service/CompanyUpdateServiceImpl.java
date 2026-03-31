package com.briefin.domain.companies.service;

import com.briefin.domain.companies.repository.CompaniesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor // 생성자 주입을 위해 추가 (Lombok 사용 시)
public class CompanyUpdateServiceImpl implements CompanyUpdateService {

    private final CompaniesRepository companiesRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updatePrice(String ticker, Long marketCap) {
        companiesRepository.updateMarketCap(ticker, BigDecimal.valueOf(marketCap));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updatePriceAndChangeRate(String ticker, double currentPrice, double changeRate, long marketCap) {
        companiesRepository.updatePrice(ticker, BigDecimal.valueOf(currentPrice), BigDecimal.valueOf(changeRate));
        if (marketCap > 0) {
            companiesRepository.updateMarketCap(ticker, BigDecimal.valueOf(marketCap));
        }
    }
}