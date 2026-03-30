package com.briefin.domain.users.service;

import com.briefin.domain.news.entity.NewsView;
import com.briefin.domain.news.repository.NewsSummaryRepository;
import com.briefin.domain.news.repository.NewsViewRepository;
import com.briefin.domain.users.dto.RecentNewsResponseDto;
import com.briefin.domain.users.dto.UserResponseDto;
import com.briefin.domain.users.entity.Users;
import com.briefin.domain.users.repository.ScrapsRepository;
import com.briefin.domain.users.repository.WatchlistRepository;
import com.briefin.domain.users.repository.UsersRepository;
import com.briefin.global.apipayload.code.status.ErrorCode;
import com.briefin.global.apipayload.exception.BriefinException;
import com.briefin.global.security.jwt.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UsersServiceImpl implements UsersService{
    private final UsersRepository usersRepository;
    private final WatchlistRepository watchlistRepository;
    private final ScrapsRepository scrapsRepository;
    private final NewsViewRepository newsViewRepository;
    private final NewsSummaryRepository newsSummaryRepository;
    private final RefreshTokenService refreshTokenService;

    @Override
    public UserResponseDto getUser(UUID userId){
        Users user = usersRepository.findById(userId)
                .orElseThrow(() ->new BriefinException(ErrorCode.USER_NOT_FOUND));
        return UserResponseDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    public RecentNewsResponseDto getRecentNews(UUID userId, int page, int size) {
        Page<NewsView> viewPage = newsViewRepository.findByUserIdOrderByViewedAtDesc(
                userId, PageRequest.of(page - 1, size)
        );

        List<Long> newsIds = viewPage.getContent().stream()
                .map(v -> v.getNews().getId())
                .toList();

        Map<Long, String> summaryMap = newsSummaryRepository.findByNewsIdIn(newsIds).stream()
                .collect(Collectors.toMap(ns -> ns.getNews().getId(), ns -> ns.getSummaryLine()));

        List<RecentNewsResponseDto.RecentNewsItem> recentList = viewPage.getContent().stream()
                .map(view -> RecentNewsResponseDto.RecentNewsItem.builder()
                        .newsId(view.getNews().getId())
                        .title(view.getNews().getTitle())
                        .summary(summaryMap.get(view.getNews().getId()))
                        .source(view.getNews().getSource())
                        .viewedAt(view.getViewedAt())
                        .build())
                .toList();

        return RecentNewsResponseDto.builder()
                .recentList(recentList)
                .totalCount(viewPage.getTotalElements())
                .build();
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        usersRepository.findById(userId)
                .orElseThrow(() -> new BriefinException(ErrorCode.USER_NOT_FOUND));

        watchlistRepository.deleteByUserId(userId);
        scrapsRepository.deleteByUserId(userId);

        newsViewRepository.deleteByUserId(userId);

        refreshTokenService.delete(userId);

        usersRepository.deleteById(userId);
    }
}
