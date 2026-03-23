package com.briefin.domain.users.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class WatchlistResponseDto {

    private List<WatchlistItem> watchlist;

    @Getter
    @Builder
    public static class WatchlistItem {
        private Long companyId;
        private String companyName;
        private String ticker;
        private String logoUrl;
        private LocalDateTime addedAt;
    }

    @Builder
    @Getter
    public static class WatchlistAddResponseDto {
        private Long companyId;
        private UUID userId;
    }
}
