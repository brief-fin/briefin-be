package com.briefin.domain.users.service;

import com.briefin.domain.users.dto.WatchlistResponseDto;

import java.util.UUID;

public interface
WatchlistService {
    WatchlistResponseDto getWatchlist(UUID userId);

    WatchlistResponseDto.WatchlistAddResponseDto addWatch(Long companyId, UUID userId);
    void removeWatch(Long companyId, UUID userId);
}
