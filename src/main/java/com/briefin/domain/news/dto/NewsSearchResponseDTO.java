package com.briefin.domain.news.dto;

import java.util.List;

public record NewsSearchResponseDTO(
        String newsId,
        String title,
        String summary,
        String category,
        String press,
        String publishedAt,
        List<String> relatedCompanies
) {}
