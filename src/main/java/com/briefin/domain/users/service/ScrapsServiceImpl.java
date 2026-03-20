package com.briefin.domain.users.service;

import com.briefin.domain.news.entity.NewsSummary;
import com.briefin.domain.news.repository.NewsSummaryRepository;
import com.briefin.domain.users.dto.ScrapNewsResponseDto;
import com.briefin.domain.users.entity.Scraps;
import com.briefin.domain.users.repository.ScrapsRepository;
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
}
