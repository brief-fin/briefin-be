package com.briefin.domain.disclosures.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatGptClient {

    @Value("${openai.api-key}")
    private String openAiKey;

    private final WebClient openAiWebClient;

    private String cleanRawText(String rawText) {
        log.info("=== RAW TEXT 앞 200자 ===\n[{}]", rawText.substring(0, Math.min(200, rawText.length())));
        String cleaned = rawText
                .replaceAll("잠시만 기다려주세요\\.\\s*", "")  // 로딩 텍스트 제거
                .replaceAll("\\b(코|유|넥|기)\\s+(?=\\S)", "");   // 접두어 제거
        log.info("=== CLEANED TEXT 앞 200자 ===\n[{}]", cleaned.substring(0, Math.min(200, cleaned.length())));
        return cleaned;
    }

    public String summarize(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            log.warn("요약할 원문이 없습니다.");
            return null;
        }

        String cleaned = cleanRawText(rawText);

        String truncated = cleaned.length() > 3000
                ? cleaned.substring(0, 3000) + "..."
                : cleaned;

        String prompt = """
            다음은 국내 상장 기업의 공시 원문입니다.
            투자자가 이해하기 쉽게 정확히 3문장으로 요약해주세요.
            반드시 각 문장을 줄바꿈으로 구분하여 3줄로 작성해주세요.
            
            [공시 내용]
            %s
            """.formatted(truncated);

        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "max_tokens", 500,
                "messages", List.of(
                        Map.of("role", "system", "content", "당신은 주식 공시 분석 전문가입니다."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        try {
            return openAiWebClient.post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + openAiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() && status.value() != 429,
                            response -> response.bodyToMono(String.class)
                                    .map(errorBody -> new RuntimeException(
                                            "OpenAI 클라이언트 오류 [%d]: %s".formatted(response.statusCode().value(), errorBody)
                                    ))
                    )
                    .onStatus(
                            HttpStatusCode::is5xxServerError,
                            response -> response.bodyToMono(String.class)
                                    .map(errorBody -> new RuntimeException(
                                            "OpenAI 서버 오류 [%d]: %s".formatted(response.statusCode().value(), errorBody)
                                    ))
                    )
                    .bodyToMono(Map.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                            .filter(ex -> ex instanceof WebClientResponseException.TooManyRequests)
                            .onRetryExhaustedThrow((spec, signal) ->
                                    new RuntimeException("OpenAI Rate Limit 초과 — 재시도 소진", signal.failure())
                            )
                    )
                    .map(response -> {
                        List<Map> choices = (List<Map>) response.get("choices");
                        if (choices == null || choices.isEmpty()) {
                            throw new RuntimeException("ChatGPT 응답 choices가 비어있습니다.");
                        }
                        Map message = (Map) choices.get(0).get("message");
                        if (message == null) {
                            throw new RuntimeException("ChatGPT 응답 message가 null입니다.");
                        }
                        String content = (String) message.get("content");

                        // \n이 없으면 문장 단위로 강제 분리
                        if (content != null && !content.contains("\n")) {
                            content = content
                                    .replaceAll("(?<=[다요])\\. ", "\n")
                                    .replaceAll("(?<=[다요])\\.$", "");
                        }

                        // 각 문장 끝 온점 통일 (없으면 추가, 있으면 유지)
                        if (content != null) {
                            content = Arrays.stream(content.split("\n"))
                                    .map(String::trim)
                                    .filter(s -> !s.isBlank())
                                    .map(s -> s.endsWith(".") ? s : s + ".")
                                    .collect(Collectors.joining("\n"));
                        }

                        return content;
                    })
                    .block();

        } catch (Exception e) {
            log.error("ChatGPT 요약 실패: {}", e.getMessage());
            return null;
        }
    }

    public String summarizeDetail(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            log.warn("상세 요약할 원문이 없습니다.");
            return null;
        }

        String cleaned = cleanRawText(rawText);

        String truncated = cleaned.length() > 3000
                ? cleaned.substring(0, 3000) + "..."
                : cleaned;

        String prompt = """
                다음은 국내 상장 기업의 공시 원문입니다.
                투자자가 이해하기 쉽도록 아래 형식에 맞춰 분석해주세요.
                
                ## 핵심 포인트
                - (첫 번째 핵심 포인트)
                - (두 번째 핵심 포인트)
                - (세 번째 핵심 포인트)
                
                ## 상세 내용
                (공시의 주요 내용을 항목별로 설명. 계약 금액, 기간, 상대방 등 구체적 수치 포함)
                
                ## 투자 의견
                (이 공시가 호재인지 악재인지, 그 이유를 2~3문장으로)
                
                [공시 내용]
                %s
                """.formatted(truncated);

        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "max_tokens", 1000,
                "messages", List.of(
                        Map.of("role", "system", "content", "당신은 주식 공시 분석 전문가입니다."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        try {
            return openAiWebClient.post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + openAiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() && status.value() != 429,
                            response -> response.bodyToMono(String.class)
                                    .map(errorBody -> new RuntimeException(
                                            "OpenAI 클라이언트 오류 [%d]: %s".formatted(response.statusCode().value(), errorBody)
                                    ))
                    )
                    .onStatus(
                            HttpStatusCode::is5xxServerError,
                            response -> response.bodyToMono(String.class)
                                    .map(errorBody -> new RuntimeException(
                                            "OpenAI 서버 오류 [%d]: %s".formatted(response.statusCode().value(), errorBody)
                                    ))
                    )
                    .bodyToMono(Map.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                            .filter(ex -> ex instanceof WebClientResponseException.TooManyRequests)
                            .onRetryExhaustedThrow((spec, signal) ->
                                    new RuntimeException("OpenAI Rate Limit 초과 — 재시도 소진", signal.failure())
                            )
                    )
                    .map(response -> {
                        List<Map> choices = (List<Map>) response.get("choices");
                        if (choices == null || choices.isEmpty()) throw new RuntimeException("choices 비어있음");
                        Map message = (Map) choices.get(0).get("message");
                        if (message == null) throw new RuntimeException("message null");
                        return (String) message.get("content");
                    })
                    .block();

        } catch (Exception e) {
            log.error("ChatGPT 요약 실패", e);
            throw new IllegalStateException("ChatGPT 요약 실패", e);
        }
    }
}