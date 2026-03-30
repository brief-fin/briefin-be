package com.briefin.domain.companies.manager;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@Slf4j
public class KisTokenManager {

    @Value("${kis.base-url}")
    private String baseUrl;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    private String accessToken;

    @PostConstruct
    public void init() {
        refreshToken();
    }

    @Scheduled(cron = "0 10 8 * * *")
    @SuppressWarnings("unchecked")
    public void refreshToken() {
        try {
            Map<String, Object> response = (Map<String, Object>) WebClient.create(baseUrl)
                    .post()
                    .uri("/oauth2/tokenP")
                    .header("Content-Type", "application/json")
                    .bodyValue(Map.of(
                            "grant_type", "client_credentials",
                            "appkey", appKey,
                            "appsecret", appSecret
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.get("access_token") != null) {
                this.accessToken = (String) response.get("access_token");
                log.info("한투 토큰 갱신 완료");
            } else {
                log.error("한투 토큰 갱신 실패: 응답 없음");
            }
        } catch (Exception e) {
            log.error("한투 토큰 갱신 실패", e);
        }
    }

    public String getToken() {
        if (accessToken == null) {
            refreshToken();
        }
        return accessToken;
    }
}