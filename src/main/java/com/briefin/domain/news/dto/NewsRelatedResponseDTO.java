package com.briefin.domain.news.dto;

public record NewsRelatedResponseDTO(
        String newsId,
        String title,
        String summary,
        String category,
        String press,
        String publishedAt
) {}
