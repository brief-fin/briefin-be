package com.briefin.domain.companies.service;

public interface CompanyUpdateService {
    /**
     * 개별 종목의 시가총액을 독립된 트랜잭션으로 업데이트합니다.
     * @param ticker 종목 코드
     * @param marketCap 시가총액 값
     */
    void updatePrice(String ticker, Long marketCap);

    /**
     * 개별 종목의 현재가, 등락률, 시가총액을 독립된 트랜잭션으로 업데이트합니다.
     */
    void updatePriceAndChangeRate(String ticker, double currentPrice, double changeRate, long marketCap);
}