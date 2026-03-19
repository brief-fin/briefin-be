package com.briefin.domain.disclosures.controller;

import com.briefin.domain.disclosures.dto.DisclosuresResponseDTO;
import com.briefin.domain.disclosures.service.DisclosuresService;
import com.briefin.global.apipayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/disclosure")
@RequiredArgsConstructor
public class DisclosuresController {

    private final DisclosuresService disclosuresService;

    // 1. 공시 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<Page<DisclosuresResponseDTO.DisclosureListResponse>>> getDisclosureList(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<DisclosuresResponseDTO.DisclosureListResponse> result =
                disclosuresService.getDisclosureList(companyId, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // 2. 공시 상세 조회
    @GetMapping("/{disclosureId}")
    public ResponseEntity<ApiResponse<DisclosuresResponseDTO.DisclosureDetailResponse>> getDisclosureDetail(
            @PathVariable Long disclosureId
    ) {
        DisclosuresResponseDTO.DisclosureDetailResponse result =
                disclosuresService.getDisclosureDetail(disclosureId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // 3. 기업별 최근 공시 조회
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<DisclosuresResponseDTO.DisclosureRecentResponse>>> getRecentDisclosures(
            @RequestParam Long companyId
    ) {
        List<DisclosuresResponseDTO.DisclosureRecentResponse> result =
                disclosuresService.getRecentDisclosures(companyId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}