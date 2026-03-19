package com.briefin.domain.disclosures.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.UUID;

public class DisclosuresResponseDTO {

    @Getter
    @Builder
    public static class DisclosureListResponse {
        private UUID disclosureId;
        private String dartId;
        private String title;
        private String disclosedAt;
        private UUID companyId;
        private String companyName;
        private String ticker;
    }

    @Getter
    @Builder
    public static class DisclosureDetailResponse {
        private UUID disclosureId;
        private String dartId;
        private String title;
        private String disclosedAt;
        private String url;
        private UUID companyId;
        private String companyName;
        private String ticker;
    }

    @Getter
    @Builder
    public static class DisclosureRecentResponse {
        private UUID disclosureId;
        private String dartId;
        private String title;
        private String disclosedAt;
    }
}