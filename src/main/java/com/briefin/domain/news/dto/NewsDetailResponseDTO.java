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
        String thumbnailUrl,
        boolean isScraped,
        List<CompanyInfo> relatedCompanies,
        List<String> relatedNews
) {
    public record CompanyInfo(String companyId, String name, String ticker) {}
}
