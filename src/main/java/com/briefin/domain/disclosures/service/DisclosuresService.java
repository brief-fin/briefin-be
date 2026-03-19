package com.briefin.domain.disclosures.service;

import com.briefin.domain.disclosures.dto.DisclosuresResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface DisclosuresService {

    Page<DisclosuresResponseDTO.DisclosureListResponse> getDisclosureList(Long companyId, int page, int size);

    DisclosuresResponseDTO.DisclosureDetailResponse getDisclosureDetail(Long disclosureId);

    List<DisclosuresResponseDTO.DisclosureRecentResponse> getRecentDisclosures(Long companyId);
}