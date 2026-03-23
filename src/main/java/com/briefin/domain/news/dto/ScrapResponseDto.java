package com.briefin.domain.news.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ScrapResponseDto {
    private Long newsId;
    private Boolean isScraped;
    private LocalDateTime scrapedAt;
}
