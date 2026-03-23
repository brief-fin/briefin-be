package com.briefin.domain.users.service;

import com.briefin.domain.news.dto.ScrapResponseDto;
import com.briefin.domain.news.entity.News;
import com.briefin.domain.news.entity.NewsSummary;
import com.briefin.domain.news.repository.NewsRepository;
import com.briefin.domain.news.repository.NewsSummaryRepository;
import com.briefin.domain.users.dto.ScrapNewsResponseDto;
import com.briefin.domain.users.entity.Scraps;
import com.briefin.domain.users.entity.Users;
import com.briefin.domain.users.repository.ScrapsRepository;
import com.briefin.domain.users.repository.UsersRepository;
import com.briefin.global.apipayload.code.status.ErrorCode;
import com.briefin.global.apipayload.exception.BriefinException;
import lombok.RequiredArgsConstructor;
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
@Transactional(readOnly = true)
public class ScrapsServiceImpl implements ScrapsService {

    private final ScrapsRepository scrapsRepository;
    private final NewsSummaryRepository newsSummaryRepository;
    private final UsersRepository usersRepository;
    private final NewsRepository newsRepository;

    @Override
    public ScrapNewsResponseDto getScrappedNews(UUID userId, int page, int size) {
        Page<Scraps> scrapsPage = scrapsRepository.findByUserIdWithNews(
                userId, PageRequest.of(page - 1, size)
        );

        List<Long> newsIds = scrapsPage.getContent().stream()
                .map(s -> s.getNews().getId())
                .toList();

        Map<Long, String> summaryMap = newsSummaryRepository.findByNewsIdIn(newsIds)
                .stream()
                .collect(Collectors.toMap(ns -> ns.getNews().getId(), NewsSummary::getSummaryLine));

        List<ScrapNewsResponseDto.ScrapNewsItem> scrapList = scrapsPage.getContent().stream()
                .map(scrap -> ScrapNewsResponseDto.ScrapNewsItem.builder()
                        .newsId(scrap.getNews().getId())
                        .title(scrap.getNews().getTitle())
                        .summary(summaryMap.get(scrap.getNews().getId()))
                        .source(scrap.getNews().getSource())
                        .scrapedAt(scrap.getCreatedAt())
                        .build())
                .toList();

        return ScrapNewsResponseDto.builder()
                .scrapList(scrapList)
                .totalCount(scrapsPage.getTotalElements())
                .build();
    }

    @Override
    @Transactional
    public ScrapResponseDto addScrap(UUID userId, Long newsId) {
        if (scrapsRepository.existsByUserIdAndNewsId(userId, newsId)) {
            throw new BriefinException(ErrorCode.NEWS_ALREADY_SCRAPED);
        }

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new BriefinException(ErrorCode.USER_NOT_FOUND));
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new BriefinException(ErrorCode.NEWS_NOT_FOUND));

        Scraps scrap;
        try {
            scrap = scrapsRepository.save(Scraps.builder()
                    .user(user)
                    .news(news)
                    .build());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BriefinException(ErrorCode.NEWS_ALREADY_SCRAPED);
        }

        return ScrapResponseDto.builder()
                .newsId(news.getId())
                .isScraped(true)
                .scrapedAt(scrap.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public ScrapResponseDto removeScrap(UUID userId, Long newsId) {
        Scraps scrap = scrapsRepository.findByUserIdAndNewsId(userId, newsId)
                .orElseThrow(() -> new BriefinException(ErrorCode.NEWS_SCRAP_NOT_FOUND));

        scrapsRepository.delete(scrap);

        return ScrapResponseDto.builder()
                .newsId(newsId)
                .isScraped(false)
                .build();
    }
}
