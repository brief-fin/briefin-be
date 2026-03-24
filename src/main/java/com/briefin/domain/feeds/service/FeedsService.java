package com.briefin.domain.feeds.service;

import com.briefin.domain.feeds.dto.FeedsResponseDTO;

import java.util.List;
import java.util.UUID;

public interface FeedsService {
    List<FeedsResponseDTO> getFeed(UUID userId, int page, int size);
}
