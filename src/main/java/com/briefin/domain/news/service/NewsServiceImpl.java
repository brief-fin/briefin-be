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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
    private final EconomicTermRepository economicTermRepository;

    @Override
    public NewsPageResponseDTO getNewsList(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("news.publishedAt").descending());

        Page<NewsSummary> summaryPage = (category == null || category.equals("all"))
                ? newsSummaryRepository.findAllWithNewsPage(pageable)
                : newsSummaryRepository.findByCategoryWithNewsPage(category, pageable);

        List<Long> newsIds = summaryPage.getContent().stream()
                .map(ns -> ns.getNews().getId())
                .toList();

        Map<Long, List<NewsCompany>> companiesMap = newsIds.isEmpty() ? Map.of() :
                newsCompanyRepository.findByNewsIdIn(newsIds)
                .stream()
                .collect(Collectors.groupingBy(nc -> nc.getNews().getId()));

        Page<NewsListResponseDTO> dtoPage = summaryPage.map(summary -> NewsConverter.toListDTO(
                summary.getNews(),
                summary,
                companiesMap.getOrDefault(summary.getNews().getId(), List.of())
        ));

        return NewsPageResponseDTO.from(dtoPage);
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
    public NewsPageResponseDTO searchNews(String q, int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "publishedAt").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<News> newsPage = newsRepository.searchByKeyword(q, pageable);

        List<Long> newsIds = newsPage.getContent().stream()
                .map(News::getId)
                .toList();

        Map<Long, NewsSummary> summaryMap = newsIds.isEmpty() ? Map.of() :
                newsSummaryRepository.findByNewsIdIn(newsIds).stream()
                        .collect(Collectors.toMap(ns -> ns.getNews().getId(), ns -> ns));

        Map<Long, List<NewsCompany>> companiesMap = newsIds.isEmpty() ? Map.of() :
                newsCompanyRepository.findByNewsIdIn(newsIds).stream()
                        .collect(Collectors.groupingBy(nc -> nc.getNews().getId()));

        Page<NewsListResponseDTO> result = newsPage.map(news -> NewsConverter.toListDTO(
                news,
                summaryMap.get(news.getId()),
                companiesMap.getOrDefault(news.getId(), List.of())
        ));

        return NewsPageResponseDTO.from(result);
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

    @Override
    public List<TermExplanationDTO> getTermExplanations(Long newsId) {
        News news = findNewsById(newsId);
        String content = news.getContent();
        if (content == null || content.isBlank()) {
            return List.of();
        }

        return economicTermRepository.findAll().stream()
                .filter(t -> content.contains(t.getTerm()))
                .map(t -> new TermExplanationDTO(t.getTerm(), t.getExplanation(), t.getOriginalExplanation()))
                .toList();
    }

    @Override
    public NewsPageResponseDTO getNewsByCompany(Long companyId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<News> newsPage = newsCompanyRepository.findNewsByCompanyId(companyId, pageable);

        List<Long> newsIds = newsPage.getContent().stream()
                .map(News::getId)
                .toList();

        Map<Long, NewsSummary> summaryMap = newsIds.isEmpty() ? Map.of() :
                newsSummaryRepository.findByNewsIdIn(newsIds).stream()
                        .collect(Collectors.toMap(ns -> ns.getNews().getId(), ns -> ns));

        Map<Long, List<NewsCompany>> companiesMap = newsIds.isEmpty() ? Map.of() :
                newsCompanyRepository.findByNewsIdIn(newsIds).stream()
                        .collect(Collectors.groupingBy(nc -> nc.getNews().getId()));

        Page<NewsListResponseDTO> result = newsPage.map(news -> NewsConverter.toListDTO(
                news,
                summaryMap.get(news.getId()),
                companiesMap.getOrDefault(news.getId(), List.of())
        ));

        return NewsPageResponseDTO.from(result);
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
