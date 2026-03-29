package com.briefin.domain.disclosures.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

public class DisclosuresResponseDTO {

    @Getter
    @Builder
    public static class DisclosureListResponse {
        private Long disclosureId;
        private String dartId;
        private String title;
        private String disclosedAt;
        private Long companyId;
        private String companyName;
        private String ticker;
        private String summary;
        private String category;
    }

    @Getter
    @Builder
    public static class DisclosurePageResponse {
        private List<DisclosureListResponse> content;
        private long totalElements;
        private int totalPages;
        private int currentPage;
        private int size;

        public static DisclosurePageResponse from(Page<DisclosureListResponse> page) {
            return DisclosurePageResponse.builder()
                    .content(page.getContent())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .currentPage(page.getNumber())
                    .size(page.getSize())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class DisclosureDetailResponse {
        private Long disclosureId;
        private String dartId;
        private String title;
        private String disclosedAt;
        private String url;
        private Long companyId;
        private String companyName;
        private String ticker;
        private String summaryDetail;       // 레거시 마크다운 형식 (파싱 실패 시 fallback)
        private List<String> keyPoints;
        private String detailedContent;
        private String sentiment;           // "호재" | "악재" | "중립"
        private String investmentAnalysis;  // 투자 관점 종합 분석 (긍정 요인 + 리스크 통합)
        private String category;
    }

    @Getter
    @Builder
    public static class DisclosureRecentResponse {
        private Long disclosureId;
        private String dartId;
        private String title;
        private String disclosedAt;
        private String summary;
        private String category;
    }
}