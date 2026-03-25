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

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatGptClient {

    @Value("${openai.api.key}")
    private String openAiKey;

    private final WebClient openAiWebClient;

    public String summarize(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            log.warn("요약할 원문이 없습니다.");
            return null;
        }

        String truncated = rawText.length() > 3000
                ? rawText.substring(0, 3000) + "..."
                : rawText;

        String prompt = """
                다음은 국내 상장 기업의 공시 원문입니다.
                투자자가 이해하기 쉽게 3~5문장으로 요약해주세요.
                
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
                    // 429 Rate Limit은 retryWhen에서 처리하므로 여기선 그 외 4xx만 에러 처리
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
                    // 429 Rate Limit 시 최대 3회, 요청마다 2초씩 늘어나는 backoff 재시도
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
                        return (String) message.get("content");
                    })
                    .block(); // 기존 동기 흐름 유지 (서비스가 동기면 block() 사용)

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

        String truncated = rawText.length() > 3000
                ? rawText.substring(0, 3000) + "..."
                : rawText;

        String prompt = """
            다음은 국내 상장 기업의 공시 원문입니다.
            투자자가 이해하기 쉽도록 아래 형식에 맞춰 분석해주세요.
            
            ## 핵심 포인트
            - (투자자 관점에서 중요한 내용 3가지를 bullet로)
            
            ## 상세 내용
            (공시의 주요 내용을 항목별로 설명. 계약 금액, 기간, 상대방 등 구체적 수치 포함)
            
            ## 투자 의견
            (이 공시가 호재인지 악재인지, 그 이유를 2~3문장으로)
            
            [공시 내용]
            %s
            """.formatted(truncated);

        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "max_tokens", 1000,  // 상세 요약은 더 길게
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