package com.briefin.domain.disclosures.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ChatGptClient {

    @Value("${openai.api.key}")
    private String openAiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String summarize(String rawText) {
        // 토큰 제한 대비 앞 3000자만 사용
        String truncated = rawText.length() > 3000
                ? rawText.substring(0, 3000) + "..."
                : rawText;

        String prompt = """
            다음은 국내 상장 기업의 공시 원문입니다.
            투자자가 이해하기 쉽게 3~5문장으로 요약해주세요.
            
            [공시 내용]
            %s
            """.formatted(truncated);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-4o-mini");
        body.put("max_tokens", 500);
        body.put("messages", List.of(
                Map.of("role", "system", "content", "당신은 주식 공시 분석 전문가입니다."),
                Map.of("role", "user", "content", prompt)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiKey);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.openai.com/v1/chat/completions",
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            List<Map> choices = (List<Map>) response.getBody().get("choices");
            Map message = (Map) choices.get(0).get("message");
            return (String) message.get("content");

        } catch (Exception e) {
            log.error("ChatGPT 요약 실패: {}", e.getMessage());
            return null;  // 실패해도 공시 저장은 계속 진행
        }
    }
}
