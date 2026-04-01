package com.briefin.domain.users.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class RecentNewsResponseDto {

    private List<RecentNewsItem> recentList;
    private long totalCount;

    @Getter
    @Builder
    public static class RecentNewsItem {
        private Long newsId;
        private String title;
        private String summary;
        private String source;
        private String thumbnailUrl;
        private LocalDateTime viewedAt;
    }
}
