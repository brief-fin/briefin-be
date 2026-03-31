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
public class LsTokenManager {

    @Value("${ls.app-key}")
    private String appKey;

    @Value("${ls.app-secret-key}")
    private String appSecretKey;

    private String accessToken;

    @PostConstruct
    public void init() {
        refreshToken();
    }

    // 매일 오전 6시에 토큰 갱신
    @Scheduled(cron = "0 6 8 * * *")
    @SuppressWarnings("unchecked")
    public void refreshToken() {
        try {
            Map<String, Object> response = (Map<String, Object>) WebClient.create("https://openapi.ls-sec.co.kr:8080")
                    .post()
                    .uri("/oauth2/token")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .bodyValue("grant_type=client_credentials&appkey=" + appKey + "&appsecretkey=" + appSecretKey + "&scope=oob")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.get("access_token") != null) {
                this.accessToken = (String) response.get("access_token");
                log.info("LS증권 토큰 갱신 완료");
            } else {
                log.error("LS증권 토큰 갱신 실패: 응답 없음");
            }
        } catch (Exception e) {
            log.error("LS증권 토큰 갱신 실패", e);
        }
    }

    public String getToken() {
        if (accessToken == null) {
            refreshToken();
        }
        return accessToken;
    }
}