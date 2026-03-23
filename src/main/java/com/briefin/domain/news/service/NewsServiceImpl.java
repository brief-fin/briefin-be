package com.briefin.domain.news.service;

import com.briefin.domain.news.converter.NewsConverter;
import com.briefin.domain.news.dto.*;
import com.briefin.domain.news.entity.*;
import com.briefin.domain.news.repository.*;
import com.briefin.global.apipayload.code.status.ErrorCode;
import com.briefin.global.apipayload.exception.BriefinException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final NewsSummaryRepository newsSummaryRepository;
    private final NewsCompanyRepository newsCompanyRepository;
    private final NewsEmbeddingRepository newsEmbeddingRepository;

    @Override
    public List<NewsListResponseDTO> getNewsList(String category) {
        List<NewsSummary> summaries = (category == null || category.equals("all"))
                ? newsSummaryRepository.findAllWithNews()
                : newsSummaryRepository.findByCategoryWithNews(category);

        List<Long> newsIds = summaries.stream()
                .map(ns -> ns.getNews().getId())
                .toList();

        Map<Long, List<NewsCompany>> companiesMap = newsCompanyRepository.findByNewsIdIn(newsIds)
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
    public NewsDetailResponseDTO getNewsDetail(Long newsId) {
        News news = findNewsById(newsId);
        List<String> relatedNewsIds = newsEmbeddingRepository.findRelatedNewsIds(newsId, 5).stream()
                .map(Object::toString)
                .toList();

        return NewsConverter.toDetailDTO(news, getSummary(newsId), getCompanies(newsId), relatedNewsIds);
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
