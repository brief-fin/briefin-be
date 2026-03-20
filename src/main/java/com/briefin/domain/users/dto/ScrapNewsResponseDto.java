package com.briefin.domain.users.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ScrapNewsResponseDto {

    private List<ScrapNewsItem> scrapList;
    private long totalCount;

    @Getter
    @Builder
    public static class ScrapNewsItem {
        private Long newsId;
        private String title;
        private String summary;
        private String source;
        private LocalDateTime scrapedAt;
    }
}
