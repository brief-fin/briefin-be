package com.briefin.domain.news.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record NewsPageResponseDTO(
        List<NewsListResponseDTO> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int size,
        boolean hasNext
) {
    public static NewsPageResponseDTO from(Page<NewsListResponseDTO> page) {
        return new NewsPageResponseDTO(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.hasNext()
        );
    }
}
