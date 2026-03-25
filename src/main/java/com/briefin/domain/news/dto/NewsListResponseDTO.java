package com.briefin.domain.news.dto;

import java.util.List;

public record NewsListResponseDTO(
        String newsId,
        String title,
        String summary,
        String category,
        String region,
        String press,
        String publishedAt,
        List<String> relatedCompanies
) {}
