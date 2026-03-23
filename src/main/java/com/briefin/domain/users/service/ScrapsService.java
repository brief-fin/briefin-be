package com.briefin.domain.users.service;

import com.briefin.domain.news.dto.ScrapResponseDto;
import com.briefin.domain.users.dto.ScrapNewsResponseDto;

import java.util.UUID;

public interface ScrapsService {
    ScrapNewsResponseDto getScrappedNews(UUID userId, int page, int size);
    ScrapResponseDto addScrap(UUID userId, Long newsId);
    ScrapResponseDto removeScrap(UUID userId, Long newsId);
}
