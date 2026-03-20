package com.briefin.domain.companies.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class KisClient {

    private final WebClient webClient;

    @Value("${kis.base-url}")
    private String baseUrl;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    private String accessToken;

    // 토큰 발급
    public String getAccessToken() {
        Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", appKey,
                "appsecret", appSecret
        );

        Map response = WebClient.create(baseUrl)
                .post()
                .uri("/oauth2/tokenP")
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        this.accessToken = (String) response.get("access_token"); // ← 이거 추가!
        return this.accessToken;
    }

    // 국내주식 섹터 조회
    public String getDomesticSector(String ticker) {
        Map response = WebClient.create(baseUrl)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/search-stock-info")
                        .queryParam("PRDT_TYPE_CD", "300")
                        .queryParam("PDNO", ticker)
                        .build())
                .header("authorization", "Bearer " + accessToken)
                .header("appkey", appKey)
                .header("appsecret", appSecret)
                .header("tr_id", "CTPF1002R")
                .header("custtype", "P")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Map output = (Map) response.get("output");
        return (String) output.get("idx_bztp_mcls_cd_name");
    }

    // 해외주식 현재가 조회
    public Map getOverseasPrice(String ticker) {
        Map response = WebClient.create(baseUrl)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/overseas-price/v1/quotations/price")
                        .queryParam("AUTH", "")
                        .queryParam("EXCD", "NAS")
                        .queryParam("SYMB", ticker)
                        .build())
                .header("authorization", "Bearer " + accessToken)
                .header("appkey", appKey)
                .header("appsecret", appSecret)
                .header("tr_id", "HHDFS00000300")
                .header("custtype", "P")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return (Map) response.get("output");
    }

    // 국내주식 종목 정보 전체 조회
    public Map<String, Object> getDomesticStockInfo(String ticker) {
        Map response = WebClient.create(baseUrl)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/search-stock-info")
                        .queryParam("PRDT_TYPE_CD", "300")
                        .queryParam("PDNO", ticker)
                        .build())
                .header("authorization", "Bearer " + accessToken)
                .header("appkey", appKey)
                .header("appsecret", appSecret)
                .header("tr_id", "CTPF1002R")
                .header("custtype", "P")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return (Map<String, Object>) response.get("output");
    }
}

