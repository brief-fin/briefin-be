package com.briefin.domain.disclosures.service;

import com.briefin.domain.disclosures.dto.DisclosuresResponseDTO;
import com.briefin.domain.disclosures.entity.Disclosures;
import com.briefin.domain.disclosures.repository.DisclosuresRepository;
import com.briefin.global.apipayload.code.status.ErrorCode;
import com.briefin.global.apipayload.exception.BriefinException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DisclosuresServiceImpl implements DisclosuresService {

    private final DisclosuresRepository disclosuresRepository;
    private final ObjectMapper objectMapper;

    // 1. 공시 목록 조회
    @Override
    public Page<DisclosuresResponseDTO.DisclosureListResponse> getDisclosureList(Long companyId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("disclosedAt").descending());

        Page<Disclosures> disclosures = (companyId != null)
                ? disclosuresRepository.findByCompanyId(companyId, pageable)
                : disclosuresRepository.findAll(pageable);

        return disclosures.map(d -> DisclosuresResponseDTO.DisclosureListResponse.builder()
                .disclosureId(d.getId())
                .dartId(d.getDartId())
                .title(d.getTitle())
                .disclosedAt(d.getDisclosedAt().toString())
                .companyId(d.getCompany().getId())
                .companyName(d.getCompany().getName())
                .ticker(d.getCompany().getTicker())
                .summary(d.getSummary())
                .keyPoints(parseKeyPoints(d.getSummaryDetail()))
                .category(d.getCategory())
                .build());
    }

    // 2. 공시 상세 조회
    @Override
    public DisclosuresResponseDTO.DisclosureDetailResponse getDisclosureDetail(Long disclosureId) {
        Disclosures disclosure = disclosuresRepository.findById(disclosureId)
                .orElseThrow(() -> new BriefinException(ErrorCode.DISCLOSURE_NOT_FOUND));

        var builder = DisclosuresResponseDTO.DisclosureDetailResponse.builder()
                .disclosureId(disclosure.getId())
                .dartId(disclosure.getDartId())
                .title(disclosure.getTitle())
                .disclosedAt(disclosure.getDisclosedAt().toString())
                .url(disclosure.getUrl())
                .companyId(disclosure.getCompany().getId())
                .companyName(disclosure.getCompany().getName())
                .ticker(disclosure.getCompany().getTicker())
                .category(disclosure.getCategory());

        parseSummaryDetail(disclosure.getSummaryDetail(), builder);

        return builder.build();
    }

    private List<String> parseKeyPoints(String rawDetail) {
        if (rawDetail == null || rawDetail.isBlank()) return Collections.emptyList();
        try {
            String jsonStr = rawDetail.strip();
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").strip();
            }
            JsonNode node = objectMapper.readTree(jsonStr);
            return toStringList(node.get("keyPoints"));
        } catch (Exception e) {
            log.warn("keyPoints 파싱 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private void parseSummaryDetail(
            String rawDetail,
            DisclosuresResponseDTO.DisclosureDetailResponse.DisclosureDetailResponseBuilder builder
    ) {
        if (rawDetail == null || rawDetail.isBlank()) return;

        try {
            // GPT가 ```json ... ``` 마크다운 코드블록으로 감싸는 경우 제거
            String jsonStr = rawDetail.strip();
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").strip();
            }

            JsonNode node = objectMapper.readTree(jsonStr);

            List<String> keyPoints = toStringList(node.get("keyPoints"));
            String detailedContent = node.has("detailedContent") ? node.get("detailedContent").asText() : null;
            String sentiment = node.has("sentiment") ? node.get("sentiment").asText() : null;
            String investmentAnalysis = node.has("investmentAnalysis") ? node.get("investmentAnalysis").asText() : null;

            builder
                    .keyPoints(keyPoints)
                    .detailedContent(detailedContent)
                    .sentiment(sentiment)
                    .investmentAnalysis(investmentAnalysis);
        } catch (Exception e) {
            log.warn("summaryDetail JSON 파싱 실패 (레거시 형식으로 응답): {}", e.getMessage());
            builder.summaryDetail(rawDetail);
        }
    }

    private List<String> toStringList(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) return Collections.emptyList();
        List<String> result = new java.util.ArrayList<>();
        arrayNode.forEach(item -> result.add(item.asText()));
        return result;
    }

    // 3. 기업별 최근 공시 조회
    @Override
    public List<DisclosuresResponseDTO.DisclosureRecentResponse> getRecentDisclosures(Long companyId) {
        List<Disclosures> disclosures = disclosuresRepository
                .findTop3ByCompanyIdOrderByDisclosedAtDesc(companyId);

        return disclosures.stream()
                .map(d -> DisclosuresResponseDTO.DisclosureRecentResponse.builder()
                        .disclosureId(d.getId())
                        .dartId(d.getDartId())
                        .title(d.getTitle())
                        .disclosedAt(d.getDisclosedAt().toString())
                        .summary(d.getSummary())
                        .category(d.getCategory())
                        .build())
                .toList();
    }
}