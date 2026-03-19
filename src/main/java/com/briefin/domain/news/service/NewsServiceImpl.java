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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final NewsSummaryRepository newsSummaryRepository;
    private final NewsCompanyRepository newsCompanyRepository;

    @Override
    public List<NewsListResponseDTO> getNewsList(String category) {
        List<News> newsList = (category == null || category.equals("all"))
                ? newsSummaryRepository.findAll().stream()
                        .map(NewsSummary::getNews)
                        .toList()
                : newsSummaryRepository.findByCategory(category).stream()
                        .map(NewsSummary::getNews)
                        .toList();

        return newsList.stream()
                .map(news -> NewsConverter.toListDTO(news, getSummary(news.getId()), getCompanies(news.getId())))
                .toList();
    }

    @Override
    public NewsDetailResponseDTO getNewsDetail(Long newsId) {
        News news = findNewsById(newsId);
        List<String> relatedNewsIds = newsRepository.findRelatedNews(newsId).stream()
                .map(n -> n.getId().toString())
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
        return newsRepository.findRelatedNews(newsId).stream()
                .map(news -> NewsConverter.toRelatedDTO(news, getSummary(news.getId())))
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
