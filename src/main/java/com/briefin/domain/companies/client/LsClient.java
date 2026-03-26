package com.briefin.domain.companies.client;

import com.briefin.domain.companies.entity.StockPrice;
import com.briefin.domain.companies.manager.LsTokenManager;
import com.briefin.domain.companies.socket.StockPriceCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public StockPrice getPreviousClosePrice(String ticker) {
        try {
            String token = lsTokenManager.getToken();
            Map<String, Object> response = (Map<String, Object>) WebClient.create(baseUrl)
                    .post()
                    .uri("/stock/chart")
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("authorization", "Bearer " + token)
                    .header("tr_cd", "t8410")
                    .header("tr_cont", "N")
                    .bodyValue(Map.of("t8410InBlock", Map.of(
                            "shcode", ticker,
                            "gubun", "2",
                            "qrycnt", 1,
                            "sdate", "",
                            "edate", "",
                            "cts_date", "",
                            "comp_yn", "N",
                            "sujung", "Y"
                    )))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || response.get("t8410OutBlock") == null) {
                log.warn("전일종가 조회 실패: {}", ticker);
                return null;
            }

            Map<String, Object> output = (Map<String, Object>) response.get("t8410OutBlock");
            double jiclose = Double.parseDouble(output.get("jiclose").toString());

            log.info("전일종가 조회: {} → {}원", ticker, jiclose);
            return new StockPrice(jiclose, 0.0);

        } catch (Exception e) {
            log.error("전일종가 조회 중 오류: {}", ticker);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public StockPrice getCurrentPrice(String ticker) {
        StockPrice cached = stockPriceCache.get(ticker);
        if (cached != null && cached.getCurrentPrice() > 0) { // ← 0이면 캐시 무시
            return cached;
        }

        // 캐시 없거나 0이면 REST API 호출
        try {
            String token = lsTokenManager.getToken();
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

            // 장 전/후라서 price가 0이면 기준가(전일종가) 사용
            if (price == 0) {
                price = Double.parseDouble(output.get("recprice").toString());
                diff = 0.0;
                log.info("장 전/후 기준가 사용: {} → {}원", ticker, price);
            }

            StockPrice stockPrice = new StockPrice(price, diff);
            stockPriceCache.update(ticker, stockPrice);
            log.info("현재가 조회: {} → {}원 ({}%)", ticker, price, diff);
            return stockPrice;

        } catch (Exception e) {
            log.error("현재가 조회 중 오류 발생: {}", ticker);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getPopularTickers() {
        try {
            String token = lsTokenManager.getToken();
            Map response = (Map) WebClient.create(baseUrl)
                    .post()
                    .uri("/stock/high-item")
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("authorization", "Bearer " + token)
                    .header("tr_cd", "t1452")
                    .header("tr_cont", "N")
                    .bodyValue(Map.of("t1452InBlock", Map.of(
                            "gubun", "0",      // 전체
                            "jnilgubun", "1",  // 당일
                            "sdiff", 0,
                            "ediff", 0,
                            "jc_num", 0,
                            "sprice", 0,
                            "eprice", 0,
                            "volume", 0,
                            "idx", 0
                    )))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || response.get("t1452OutBlock1") == null) {
                throw new RuntimeException("거래량 상위 종목 조회 실패");
            }

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("t1452OutBlock1");
            return items.stream()
                    .map(item -> item.get("shcode").toString())
                    .limit(10)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("인기 기업 조회 중 오류 발생", e);
            return List.of();
        }
    }
}