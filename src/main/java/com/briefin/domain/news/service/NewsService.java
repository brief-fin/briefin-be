package com.briefin.domain.news.service;

import com.briefin.domain.news.dto.*;

import java.util.List;
import java.util.UUID;


public interface NewsService {

    List<NewsListResponseDTO> getNewsList(String category);

    NewsDetailResponseDTO getNewsDetail(Long newsId, UUID userId);

    List<NewsSearchResponseDTO> searchNews(String q);

    List<NewsRelatedResponseDTO> getRelatedNews(Long newsId);

    HomeNewsResponseDTO getHomeNews();
}
