package com.briefin.domain.companies.dto;

public record CompanyTimelineItemDTO(
        String type,        // "공시" | "뉴스"
        String id,
        String title,
        String summary,
        String category,
        String date,
        String sentiment    // 공시: "호재" | "악재" | "중립", 뉴스: null
) {}
