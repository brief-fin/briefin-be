package com.briefin.domain.news.service;

import com.briefin.domain.news.converter.NewsConverter;
import com.briefin.domain.news.dto.*;
import com.briefin.domain.news.entity.*;
import com.briefin.domain.news.repository.*;
import com.briefin.domain.users.repository.ScrapsRepository;
import com.briefin.global.apipayload.code.status.ErrorCode;
import com.briefin.global.apipayload.exception.BriefinException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final NewsSummaryRepository newsSummaryRepository;
    private final NewsCompanyRepository newsCompanyRepository;
    private final NewsEmbeddingRepository newsEmbeddingRepository;
    private final NewsViewRepository newsViewRepository;
    private final ScrapsRepository scrapsRepository;

    @Override
    public List<NewsListResponseDTO> getNewsList(String category) {
        List<NewsSummary> summaries = (category == null || category.equals("all"))
                ? newsSummaryRepository.findAllWithNews()
                : newsSummaryRepository.findByCategoryWithNews(category);

        List<Long> newsIds = summaries.stream()
                .map(ns -> ns.getNews().getId())
                .toList();

        Map<Long, List<NewsCompany>> companiesMap = newsIds.isEmpty() ? Map.of() :
                newsCompanyRepository.findByNewsIdIn(newsIds)
                .stream()
                .collect(Collectors.groupingBy(nc -> nc.getNews().getId()));

        return summaries.stream()
                .map(summary -> NewsConverter.toListDTO(
                        summary.getNews(),
                        summary,
                        companiesMap.getOrDefault(summary.getNews().getId(), List.of())
                ))
                .toList();
    }

    @Override
    @Transactional
    public NewsDetailResponseDTO getNewsDetail(Long newsId, UUID userId) {
        News news = findNewsById(newsId);

        try {
            if (!newsViewRepository.existsByUserIdAndNewsId(userId, newsId)) {
                newsViewRepository.save(NewsView.builder()
                        .userId(userId)
                        .news(news)
                        .viewedAt(LocalDateTime.now())
                        .build());
            }
        } catch (DataIntegrityViolationException ignored) {
            // 동시 요청으로 인한 unique constraint 위반 — 이미 저장된 것으로 간주
        }

        List<String> relatedNewsIds = newsEmbeddingRepository.findRelatedNewsIds(newsId, 5).stream()
                .map(Object::toString)
                .toList();

        boolean isScraped = userId != null && scrapsRepository.existsByUserIdAndNewsId(userId, newsId);

        return NewsConverter.toDetailDTO(news, getSummary(newsId), getCompanies(newsId), relatedNewsIds, isScraped);
    }

    @Override
    public List<NewsSearchResponseDTO> searchNews(String q) {
        return newsRepository.searchByKeyword(q).stream()
                .map(news -> NewsConverter.toSearchDTO(news, getSummary(news.getId()), getCompanies(news.getId())))
                .toList();
    }

    @Override
    public List<NewsRelatedResponseDTO> getRelatedNews(Long newsId) {
        findNewsById(newsId);
        List<Long> relatedIds = newsEmbeddingRepository.findRelatedNewsIds(newsId, 5);
        Map<Long, News> newsMap = newsRepository.findAllById(relatedIds).stream()
                .collect(Collectors.toMap(News::getId, n -> n));
        return relatedIds.stream()
                .filter(newsMap::containsKey)
                .map(id -> NewsConverter.toRelatedDTO(newsMap.get(id), getSummary(id)))
                .toList();
    }

    @Override
    public List<NewsTimelineItemDTO> getNewsTimeline(Long newsId) {
        News current = findNewsById(newsId);
        List<Long> timelineIds = newsEmbeddingRepository.findTimelineNewsIds(newsId, 20);

        Map<Long, News> newsMap = newsRepository.findAllById(timelineIds).stream()
                .collect(Collectors.toMap(News::getId, n -> n));

        List<Long> allIds = new java.util.ArrayList<>(timelineIds);
        allIds.add(newsId);
        Map<Long, NewsSummary> summaryMap = newsSummaryRepository.findByNewsIdIn(allIds).stream()
                .collect(Collectors.toMap(ns -> ns.getNews().getId(), ns -> ns));

        List<NewsTimelineItemDTO> timeline = timelineIds.stream()
                .filter(newsMap::containsKey)
                .map(id -> NewsConverter.toTimelineItemDTO(newsMap.get(id), summaryMap.get(id), false))
                .collect(Collectors.toList());

        // 현재 기사를 날짜 순서에 맞는 위치에 삽입
        NewsTimelineItemDTO currentItem = NewsConverter.toTimelineItemDTO(current, summaryMap.get(newsId), true);
        int insertIndex = 0;
        for (int i = 0; i < timeline.size(); i++) {
            String timelineAt = timeline.get(i).publishedAt();
            String currentAt = currentItem.publishedAt();
            if (timelineAt != null && currentAt != null && timelineAt.compareTo(currentAt) > 0) {
                break;
            }
            insertIndex = i + 1;
        }
        timeline.add(insertIndex, currentItem);

        return timeline;
    }

    @Override
    public HomeNewsResponseDTO getHomeNews() {
        List<NewsSummary> domesticSummaries = newsSummaryRepository.findTop3ByRegionWithNews("국내", PageRequest.of(0, 3));
        List<NewsSummary> foreignSummaries = newsSummaryRepository.findTop3ByRegionWithNews("해외", PageRequest.of(0, 3));

        List<Long> newsIds = java.util.stream.Stream.concat(domesticSummaries.stream(), foreignSummaries.stream())
                .map(ns -> ns.getNews().getId())
                .toList();

        Map<Long, List<NewsCompany>> companiesMap = newsIds.isEmpty() ? Map.of() :
                newsCompanyRepository.findByNewsIdIn(newsIds)
                .stream()
                .collect(Collectors.groupingBy(nc -> nc.getNews().getId()));

        List<NewsListResponseDTO> domestic = domesticSummaries.stream()
                .map(ns -> NewsConverter.toListDTO(ns.getNews(), ns, companiesMap.getOrDefault(ns.getNews().getId(), List.of())))
                .toList();

        List<NewsListResponseDTO> foreign = foreignSummaries.stream()
                .map(ns -> NewsConverter.toListDTO(ns.getNews(), ns, companiesMap.getOrDefault(ns.getNews().getId(), List.of())))
                .toList();

        return new HomeNewsResponseDTO(domestic, foreign);
    }

    private News findNewsById(Long newsId) {
        return newsRepository.findById(newsId)
                .orElseThrow(() -> new BriefinException(ErrorCode.NEWS_NOT_FOUND));
    }

    private NewsSummary getSummary(Long newsId) {
        return newsSummaryRepository.findByNewsId(newsId).orElse(null);
    }

    private List<NewsCompany> getCompanies(Long newsId) {
        return newsCompanyRepository.findByNewsId(newsId);
    }
}
