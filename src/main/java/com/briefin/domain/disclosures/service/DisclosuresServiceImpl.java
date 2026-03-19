package com.briefin.domain.disclosures.service;

import com.briefin.domain.disclosures.dto.DisclosuresResponseDTO;
import com.briefin.domain.disclosures.entity.Disclosures;
import com.briefin.domain.disclosures.repository.DisclosuresRepository;
import com.briefin.global.apipayload.code.status.ErrorCode;
import com.briefin.global.apipayload.exception.BriefinException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DisclosuresServiceImpl implements DisclosuresService {

    private final DisclosuresRepository disclosuresRepository;

    // 1. 공시 목록 조회
    @Override
    public Page<DisclosuresResponseDTO.DisclosureListResponse> getDisclosureList(UUID companyId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("disclosedAt").descending());

        Page<Disclosures> disclosures = (companyId != null)
                ? disclosuresRepository.findByCompanyId(companyId, pageable)
                : disclosuresRepository.findAll(pageable);

        return disclosures.map(d -> DisclosuresResponseDTO.DisclosureListResponse.builder()
                .disclosureId(d.getId())
                .dartId(d.getDartId())
                .title(d.getTitle())
                .disclosedAt(d.getDisclosedAt().toString())
                //.companyId(d.getCompany().getId())
                //.companyName(d.getCompany().getName())
                //.ticker(d.getCompany().getTicker())
                .build());
    }

    // 2. 공시 상세 조회
    @Override
    public DisclosuresResponseDTO.DisclosureDetailResponse getDisclosureDetail(UUID disclosureId) {
        Disclosures disclosure = disclosuresRepository.findById(disclosureId)
                .orElseThrow(() -> new BriefinException(ErrorCode.DISCLOSURE_NOT_FOUND));

        return DisclosuresResponseDTO.DisclosureDetailResponse.builder()
                .disclosureId(disclosure.getId())
                .dartId(disclosure.getDartId())
                .title(disclosure.getTitle())
                .disclosedAt(disclosure.getDisclosedAt().toString())
                .url(disclosure.getUrl())
                //.companyId(disclosure.getCompany().getId())
                //.companyName(disclosure.getCompany().getName())
                //.ticker(disclosure.getCompany().getTicker())
                .build();
    }

    // 3. 기업별 최근 공시 조회
    @Override
    public List<DisclosuresResponseDTO.DisclosureRecentResponse> getRecentDisclosures(UUID companyId) {
        List<Disclosures> disclosures = disclosuresRepository
                .findTop3ByCompanyIdOrderByDisclosedAtDesc(companyId);

        return disclosures.stream()
                .map(d -> DisclosuresResponseDTO.DisclosureRecentResponse.builder()
                        .disclosureId(d.getId())
                        .dartId(d.getDartId())
                        .title(d.getTitle())
                        .disclosedAt(d.getDisclosedAt().toString())
                        .build())
                .toList();
    }
}