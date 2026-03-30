package com.briefin.domain.news.dto;

import java.util.List;

public record
NewsDetailResponseDTO(
        String newsId,
        String title,
        String content,
        String summary,
        String category,
        String press,
        String publishedAt,
        String originalUrl,
        boolean isScraped,
        List<String> relatedCompanies,
        List<String> relatedNews
) {}
