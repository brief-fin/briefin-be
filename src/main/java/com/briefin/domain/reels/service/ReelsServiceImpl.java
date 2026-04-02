package com.briefin.domain.reels.service;

import com.briefin.domain.news.entity.News;
import com.briefin.domain.news.entity.NewsCompany;
import com.briefin.domain.news.entity.NewsSummary;
import com.briefin.domain.news.repository.NewsCompanyRepository;
import com.briefin.domain.news.repository.NewsRepository;
import com.briefin.domain.news.repository.NewsSummaryRepository;
import com.briefin.domain.reels.dto.ReelsResponseDTO;
import com.briefin.domain.reels.repository.ReelsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReelsServiceImpl implements ReelsService {

    private final ReelsRepository reelsRepository;
    private final NewsRepository newsRepository;
    private final NewsSummaryRepository newsSummaryRepository;
    private final NewsCompanyRepository newsCompanyRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<ReelsResponseDTO> getFeed(UUID userId) {
        List<Long> newsIds = reelsRepository.findPersonalizedFeed(userId);

        if (newsIds.isEmpty()) {
            newsIds = reelsRepository.findFallbackFeed(userId);
        }

        Map<Long, News> newsMap = newsRepository.findAllById(newsIds).stream()
                .collect(Collectors.toMap(News::getId, n -> n));

        Map<Long, NewsSummary> summaryMap = newsSummaryRepository.findByNewsIdIn(newsIds).stream()
                .collect(Collectors.toMap(s -> s.getNews().getId(), s -> s));

        Map<Long, List<NewsCompany>> companiesMap = newsCompanyRepository.findByNewsIdIn(newsIds).stream()
                .collect(Collectors.groupingBy(nc -> nc.getNews().getId()));

        return newsIds.stream()
                .filter(newsMap::containsKey)
                .map(id -> toReelsDTO(newsMap.get(id), summaryMap.get(id), companiesMap.getOrDefault(id, List.of())))
                .toList();
    }

    private ReelsResponseDTO toReelsDTO(News news, NewsSummary summary, List<NewsCompany> companies) {
        return new ReelsResponseDTO(
                news.getId().toString(),
                news.getTitle(),
                summary != null ? summary.getSummaryLine() : null,
                summary != null ? summary.getCategory() : null,
                news.getSource(),
                news.getPublishedAt() != null ? news.getPublishedAt().format(DATE_FORMATTER) : null,
                companies.stream().map(nc -> nc.getCompany().getName()).toList(),
                news.getThumbnailUrl()
        );
    }
}
