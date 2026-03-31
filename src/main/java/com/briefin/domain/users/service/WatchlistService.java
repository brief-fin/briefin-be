package com.briefin.domain.users.service;

import com.briefin.domain.users.dto.WatchlistResponseDto;

import java.util.UUID;

public interface
WatchlistService {
    WatchlistResponseDto getWatchlist(UUID userId);

    /** 이미 관심 등록된 경우에도 예외 없이 성공(멱등). */
    void addWatch(Long companyId, UUID userId);
    void removeWatch(Long companyId, UUID userId);
}
