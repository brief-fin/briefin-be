package com.briefin.domain.users.service;

import com.briefin.domain.companies.entity.Companies;
import com.briefin.domain.companies.repository.CompaniesRepository;
import com.briefin.domain.users.dto.WatchlistResponseDto;
import com.briefin.domain.users.entity.Users;
import com.briefin.domain.users.entity.Watchlist;
import com.briefin.domain.users.repository.UsersRepository;
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
    private final CompaniesRepository companiesRepository;
    private final UsersRepository usersRepository;

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

    @Transactional
    public WatchlistResponseDto.WatchlistAddResponseDto addWatch(Long companyId, UUID userId) {
        Companies company = companiesRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("기업을 찾을 수 없습니다."));

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        boolean alreadyWatched = watchlistRepository.existsByUserIdAndCompanyId(userId, companyId);
        if (alreadyWatched) {
            throw new RuntimeException("이미 관심 등록된 기업입니다.");
        }

        Watchlist watchlist = Watchlist.builder()
                .user(user)
                .company(company)
                .build();

        watchlistRepository.save(watchlist);

        return WatchlistResponseDto.WatchlistAddResponseDto.builder()
                .companyId(companyId)
                .userId(userId)
                .build();
    }

    @Transactional
    public void removeWatch(Long companyId, UUID userId) {
        Watchlist watchlist = watchlistRepository.findByUserIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new RuntimeException("관심 등록된 기업이 없습니다."));

        watchlistRepository.delete(watchlist);
    }
}
