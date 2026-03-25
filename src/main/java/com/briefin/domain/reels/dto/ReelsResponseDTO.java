package com.briefin.domain.reels.dto;

import java.util.List;

public record ReelsResponseDTO(
        String newsId,
        String title,
        String summary,
        String category,
        String press,
        String publishedAt,
        List<String> relatedCompanies
) {}
