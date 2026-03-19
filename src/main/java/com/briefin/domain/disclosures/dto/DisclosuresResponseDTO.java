package com.briefin.domain.disclosures.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

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
    }

    @Getter
    @Builder
    public static class DisclosureRecentResponse {
        private Long disclosureId;
        private String dartId;
        private String title;
        private String disclosedAt;
    }
}