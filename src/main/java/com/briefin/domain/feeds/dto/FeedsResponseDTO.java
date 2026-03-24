package com.briefin.domain.feeds.dto;

import java.util.List;

public record FeedsResponseDTO(
        String newsId,
        String title,
        String summary,
        String category,
        String press,
        String publishedAt,
        String thumbnailUrl,
        List<String> relatedCompanies
) {}
