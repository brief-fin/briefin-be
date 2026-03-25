package com.briefin.domain.companies.client;

import com.briefin.domain.companies.entity.StockPrice;
import com.briefin.domain.companies.manager.LsTokenManager;
import com.briefin.domain.companies.socket.StockPriceCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@Slf4j
public class LsClient {

    @Value("${ls.base-url}")
    private String baseUrl;

    private final LsTokenManager lsTokenManager;

    private final StockPriceCache stockPriceCache;

    public LsClient(LsTokenManager lsTokenManager, StockPriceCache stockPriceCache) {
        this.lsTokenManager = lsTokenManager;
        this.stockPriceCache = stockPriceCache;
    }

    @SuppressWarnings("unchecked")
    public StockPrice getCurrentPrice(String ticker) {
        StockPrice cached = stockPriceCache.get(ticker);
        String token = lsTokenManager.getToken();

        if (cached != null) {
            return cached;
        }

        try {
            Map<String, Object> response = (Map<String, Object>) WebClient.create(baseUrl)
                    .post()
                    .uri("/stock/market-data")
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("authorization", "Bearer " + token)
                    .header("tr_cd", "t1102")
                    .header("tr_cont", "N")
                    .bodyValue(Map.of("t1102InBlock", Map.of("shcode", ticker)))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || response.get("t1102OutBlock") == null) {
                log.warn("현재가 조회 실패: {}", ticker);
                return null;
            }

            Map<String, Object> output = (Map<String, Object>) response.get("t1102OutBlock");
            double price = Double.parseDouble(output.get("price").toString());
            double diff = Double.parseDouble(output.get("diff").toString());

            StockPrice stockPrice = new StockPrice(price, diff);
            stockPriceCache.update(ticker, stockPrice);
            log.info("현재가 조회: {} → {}원 ({}%)", ticker, price, diff);
            return stockPrice;

        } catch (Exception e) {
            log.error("현재가 조회 중 오류 발생: {}", ticker);
            return null;
        }
    }
}