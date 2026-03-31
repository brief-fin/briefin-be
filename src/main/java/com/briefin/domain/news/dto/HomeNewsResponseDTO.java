package com.briefin.domain.news.dto;

import java.util.List;

public record HomeNewsResponseDTO(
        List<NewsListResponseDTO> domesticNews,
        List<NewsListResponseDTO> foreignNews
) {}
