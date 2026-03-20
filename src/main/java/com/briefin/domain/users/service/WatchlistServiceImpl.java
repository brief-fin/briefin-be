package com.briefin.domain.users.service;

import com.briefin.domain.users.dto.WatchlistResponseDto;
import com.briefin.domain.users.entity.Watchlist;
import com.briefin.domain.users.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WatchlistServiceImpl implements WatchlistService {

    private final WatchlistRepository watchlistRepository;

    @Override
    public WatchlistResponseDto getWatchlist(UUID userId) {
        List<Watchlist> watchlist = watchlistRepository.findByUserIdWithCompany(userId);

        List<WatchlistResponseDto.WatchlistItem> items = watchlist.stream()
                .map(w -> WatchlistResponseDto.WatchlistItem.builder()
                        .companyId(w.getCompany().getId())
                        .companyName(w.getCompany().getName())
                        .ticker(w.getCompany().getTicker())
                        .logoUrl(w.getCompany().getLogoUrl())
                        .addedAt(w.getCreatedAt())
                        .build())
                .toList();

        return WatchlistResponseDto.builder()
                .watchlist(items)
                .build();
    }
}
