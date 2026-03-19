package com.briefin.domain.news.service;

import com.briefin.domain.news.dto.*;

import java.util.List;

public interface NewsService {

    List<NewsListResponseDTO> getNewsList(String category);

    NewsDetailResponseDTO getNewsDetail(Long newsId);

    List<NewsSearchResponseDTO> searchNews(String q);

    List<NewsRelatedResponseDTO> getRelatedNews(Long newsId);
}
