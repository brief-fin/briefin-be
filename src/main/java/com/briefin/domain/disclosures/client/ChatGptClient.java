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
        String cleaned = rawText
                .replaceAll("잠시만 기다려주세요\\.\\s*", "")  // 로딩 텍스트 제거
                .replaceAll("\\b(코|유|넥|기)\\s+(?=\\S)", "");   // 접두어 제거
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
            모든 문장은 "~습니다", "~됩니다" 등 합쇼체(존댓말)로 통일하고, "~이다", "~한다" 등 평서체는 절대 사용하지 마세요.

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
                반드시 아래 JSON 형식으로만 응답하세요. JSON 외 다른 텍스트는 절대 포함하지 마세요.

                {
                  "keyPoints": ["핵심 포인트 1문장", "핵심 포인트 1문장", "핵심 포인트 1문장"],
                  "detailedContent": "공시의 구체적인 내용. 계약 금액·기간·상대방·조건 등 수치와 사실을 최대한 상세히 기술. 6~8문장.",
                  "sentiment": "호재",
                  "investmentAnalysis": "긍정 요인과 리스크를 모두 포함한 투자 관점 분석. 2~3문장."
                }

                규칙:
                - 모든 문장은 반드시 "~습니다", "~됩니다", "~있습니다" 등 합쇼체(존댓말)로 통일. "~이다", "~한다", "~된다" 등 평서체 절대 사용 금지
                - keyPoints: 반드시 3개, 고등학생도 이해할 수 있는 쉬운 말로 각 한 문장
                - detailedContent: 공시에 담긴 핵심 수치(금액, 비율, 기간, 수량 등)와 사실을 빠짐없이 포함. 배경·목적·조건·향후 일정 등 맥락도 상세히 서술
                - sentiment: "호재" / "악재" / "중립" 중 하나만. 단어만 출력, 다른 텍스트 절대 포함 금지
                - investmentAnalysis: sentiment 단어로 시작하지 말 것. 긍정 요인과 리스크 근거를 바로 서술

                [공시 내용]
                %s
                """.formatted(truncated);

        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "max_tokens", 1500,
                "messages", List.of(
                        Map.of("role", "system", "content", "당신은 주식 공시 분석 전문가입니다. 요청한 JSON 형식 외에 다른 텍스트를 절대 출력하지 않습니다."),
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