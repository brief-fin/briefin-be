package com.briefin.domain.reels.service;

import com.briefin.domain.reels.dto.ReelsResponseDTO;

import java.util.List;
import java.util.UUID;

public interface ReelsService {
    List<ReelsResponseDTO> getFeed(UUID userId);
}
