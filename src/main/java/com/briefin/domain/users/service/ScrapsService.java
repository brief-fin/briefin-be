package com.briefin.domain.users.service;

import com.briefin.domain.users.dto.ScrapNewsResponseDto;

import java.util.UUID;

public interface ScrapsService {
    ScrapNewsResponseDto getScrappedNews(UUID userId, int page, int size);
}
