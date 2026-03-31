package com.briefin.domain.users.service;

import com.briefin.domain.companies.entity.Companies;
import com.briefin.domain.companies.repository.CompaniesRepository;
import com.briefin.domain.users.dto.WatchlistResponseDto;
import com.briefin.domain.users.entity.Users;
import com.briefin.domain.users.entity.Watchlist;
import com.briefin.domain.users.repository.UsersRepository;
import com.briefin.domain.users.repository.WatchlistRepository;
import com.briefin.global.apipayload.code.status.ErrorCode;
import com.briefin.global.apipayload.exception.BriefinException;
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
                        .name(w.getCompany().getName())
                        .sector(w.getCompany().getSector())
                        .ticker(w.getCompany().getTicker())
                        .logoUrl(w.getCompany().getLogoUrl())
                        .addedAt(w.getCreatedAt())
                        .build())
                .toList();

        return WatchlistResponseDto.builder()
                .watchlist(items)
                .build();
    }

    @Override
    @Transactional
    public void addWatch(Long companyId, UUID userId) {
        Companies company = companiesRepository.findById(companyId)
                .orElseThrow(() -> new BriefinException(ErrorCode.COMPANY_NOT_FOUND));

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new BriefinException(ErrorCode.USER_NOT_FOUND));

        if (watchlistRepository.existsByUser_IdAndCompany_Id(userId, companyId)) {
            return;
        }

        watchlistRepository.save(Watchlist.builder()
                .user(user)
                .company(company)
                .build());
    }

    @Override
    @Transactional
    public void removeWatch(Long companyId, UUID userId) {
        watchlistRepository.findByUser_IdAndCompany_Id(userId, companyId)
                .ifPresent(watchlistRepository::delete);
    }
}
