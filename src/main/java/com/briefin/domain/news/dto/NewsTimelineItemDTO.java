package com.briefin.domain.news.dto;

public record NewsTimelineItemDTO(
        String newsId,
        String title,
        String summary,
        String category,
        String press,
        String thumbnailUrl,
        String publishedAt,
        boolean isCurrent
) {}
