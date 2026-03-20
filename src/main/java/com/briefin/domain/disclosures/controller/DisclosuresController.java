package com.briefin.domain.disclosures.controller;

import com.briefin.domain.disclosures.dto.DisclosuresResponseDTO;
import com.briefin.domain.disclosures.service.DisclosureCollectService;
import com.briefin.domain.disclosures.service.DisclosuresService;
import com.briefin.global.apipayload.ApiResponse;
import com.briefin.global.apipayload.code.status.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/disclosures")
@RequiredArgsConstructor
public class DisclosuresController {

    private final DisclosuresService disclosuresService;
    private final DisclosureCollectService disclosureCollectService;

    // 공시 목록 조회
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

    // 공시 상세 조회
    @GetMapping("/{disclosureId}")
    public ResponseEntity<ApiResponse<DisclosuresResponseDTO.DisclosureDetailResponse>> getDisclosureDetail(
            @PathVariable Long disclosureId
    ) {
        DisclosuresResponseDTO.DisclosureDetailResponse result =
                disclosuresService.getDisclosureDetail(disclosureId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // 기업별 최근 공시 조회
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<DisclosuresResponseDTO.DisclosureRecentResponse>>> getRecentDisclosures(
            @RequestParam Long companyId
    ) {
        List<DisclosuresResponseDTO.DisclosureRecentResponse> result =
                disclosuresService.getRecentDisclosures(companyId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // 기업 코드
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<?>> syncCorpCodes() {
        try {
            disclosureCollectService.syncCorpCodes();
            return ResponseEntity.ok(ApiResponse.success("동기화 완료"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
        }
    }

    // 전체 공시 수집
    @PostMapping("/collect")
    public ResponseEntity<ApiResponse<?>> collectDisclosures(
            @RequestParam String startDate,
            @RequestParam String endDate
    ) {
        try {
            disclosureCollectService.collectAll(startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success("공시 수집 완료"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
        }
    }

    // 기업별 공시 수집
    @PostMapping("/collect/{corpCode}")
    public ResponseEntity<ApiResponse<?>> collectByCorpCode(
            @PathVariable String corpCode,
            @RequestParam String startDate,
            @RequestParam String endDate
    ) {
        try {
            disclosureCollectService.collectByCorpCode(corpCode, startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success("기업별 공시 수집 완료"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
        }
    }
}