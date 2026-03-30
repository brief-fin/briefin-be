package com.briefin.domain.companies.client;

import com.briefin.domain.companies.dto.PopularCompanyDto;
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
            return new StockPrice(jiclose, 0.0, 0L);

        } catch (Exception e) {
            log.error("전일종가 조회 중 오류: {}", ticker);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public StockPrice getCurrentPrice(String ticker) {
        StockPrice cached = stockPriceCache.get(ticker);
        if (cached != null && cached.getCurrentPrice() > 0) {
            return cached;
        }

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
            long marketCap = Long.parseLong(output.get("total").toString());

            if (price == 0) {
                price = Double.parseDouble(output.get("recprice").toString());
                diff = 0.0;
                log.info("장 전/후 기준가 사용: {} → {}원", ticker, price);
            }

            StockPrice stockPrice = new StockPrice(price, diff, marketCap);
            stockPriceCache.update(ticker, stockPrice);
            log.info("현재가 조회: {} → {}원 ({}%) 시가총액: {}억", ticker, price, diff, marketCap);
            return stockPrice;

        } catch (Exception e) {
            log.error("현재가 조회 중 오류 발생: {}", ticker);
            return null;
        }
    }

    // 등락율 상위 (t1441)
    @SuppressWarnings("unchecked")
    public List<PopularCompanyDto> getTopDiffTickers() {
        try {
            String token = lsTokenManager.getToken();
            Map response = (Map) WebClient.create(baseUrl)
                    .post()
                    .uri("/stock/high-item")
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("authorization", "Bearer " + token)
                    .header("tr_cd", "t1441")
                    .header("tr_cont", "N")
                    .bodyValue(Map.of("t1441InBlock", Map.of(
                            "gubun1", "0",
                            "gubun2", "0",
                            "gubun3", "0",
                            "jc_num", 0,
                            "sprice", 0,
                            "eprice", 0,
                            "volume", 0,
                            "idx", 0,
                            "jc_num2", 0,
                            "exchgubun", "K"
                    )))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || response.get("t1441OutBlock1") == null) {
                throw new RuntimeException("등락율 상위 종목 조회 실패");
            }

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("t1441OutBlock1");
            return items.stream()
                    .map(item -> new PopularCompanyDto(
                            item.get("shcode").toString(),
                            Double.parseDouble(item.get("diff").toString())
                    ))
                    .limit(10)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("등락율 상위 조회 중 오류 발생", e);
            return List.of();
        }
    }

    // 시가총액 상위 (t1444)
    @SuppressWarnings("unchecked")
    public List<PopularCompanyDto> getTopMarketCapTickers() {
        try {
            String token = lsTokenManager.getToken();
            Map response = (Map) WebClient.create(baseUrl)
                    .post()
                    .uri("/stock/high-item")
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("authorization", "Bearer " + token)
                    .header("tr_cd", "t1444")
                    .header("tr_cont", "N")
                    .bodyValue(Map.of("t1444InBlock", Map.of(
                            "upcode", "001",
                            "idx", 0
                    )))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || response.get("t1444OutBlock1") == null) {
                throw new RuntimeException("시가총액 상위 종목 조회 실패");
            }

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("t1444OutBlock1");
            return items.stream()
                    .map(item -> new PopularCompanyDto(
                            item.get("shcode").toString(),
                            Double.parseDouble(item.get("diff").toString())
                    ))
                    .limit(10)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("시가총액 상위 조회 중 오류 발생", e);
            return List.of();
        }
    }

    // 거래량 상위 (t1452)
    @SuppressWarnings("unchecked")
    public List<PopularCompanyDto> getTopVolumeTickers() {
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
                            "gubun", "0",
                            "jnilgubun", "1",
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
                    .map(item -> new PopularCompanyDto(
                            item.get("shcode").toString(),
                            Double.parseDouble(item.get("diff").toString())
                    ))
                    .limit(10)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("거래량 상위 조회 중 오류 발생", e);
            return List.of();
        }
    }

    // 거래대금 상위 (t1463)
    @SuppressWarnings("unchecked")
    public List<PopularCompanyDto> getTopValueTickers() {
        try {
            String token = lsTokenManager.getToken();
            Map response = (Map) WebClient.create(baseUrl)
                    .post()
                    .uri("/stock/high-item")
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("authorization", "Bearer " + token)
                    .header("tr_cd", "t1463")
                    .header("tr_cont", "N")
                    .bodyValue(Map.of("t1463InBlock", Map.of(
                            "gubun", "0",
                            "jnilgubun", "0",
                            "jc_num", 0,
                            "sprice", 0,
                            "eprice", 0,
                            "volume", 0,
                            "idx", 0,
                            "jc_num2", 0,
                            "exchgubun", "K"
                    )))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || response.get("t1463OutBlock1") == null) {
                throw new RuntimeException("거래대금 상위 종목 조회 실패");
            }

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("t1463OutBlock1");
            return items.stream()
                    .map(item -> new PopularCompanyDto(
                            item.get("shcode").toString(),
                            Double.parseDouble(item.get("diff").toString())
                    ))
                    .limit(10)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("거래대금 상위 조회 중 오류 발생", e);
            return List.of();
        }
    }
}