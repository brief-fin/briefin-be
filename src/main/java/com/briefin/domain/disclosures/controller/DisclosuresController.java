package com.briefin.domain.disclosures.controller;

import com.briefin.domain.disclosures.dto.DisclosuresResponseDTO;
import com.briefin.domain.disclosures.service.DisclosureCollectService;
import com.briefin.domain.disclosures.service.DisclosuresService;
import com.briefin.global.apipayload.ApiResponse;
import com.briefin.global.apipayload.code.status.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/disclosures")
@RequiredArgsConstructor
public class DisclosuresController {

    private final DisclosuresService disclosuresService;
    private final DisclosureCollectService disclosureCollectService;

    @Operation(summary = "공시 목록 조회", description = "companyId 미전달 시 전체 공시 반환")
    @GetMapping
    public ResponseEntity<ApiResponse<DisclosuresResponseDTO.DisclosurePageResponse>> getDisclosureList(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<DisclosuresResponseDTO.DisclosureListResponse> result =
                disclosuresService.getDisclosureList(companyId, page, size);
        return ResponseEntity.ok(ApiResponse.success(DisclosuresResponseDTO.DisclosurePageResponse.from(result)));
    }

    @Operation(summary = "기업별 최근 공시 조회", description = "companyId에 해당하는 최근 공시 목록 반환")
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<DisclosuresResponseDTO.DisclosureRecentResponse>>> getRecentDisclosures(
            @RequestParam Long companyId
    ) {
        List<DisclosuresResponseDTO.DisclosureRecentResponse> result =
                disclosuresService.getRecentDisclosures(companyId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "공시 상세 조회", description = "공시 ID로 단건 상세 정보 반환")
    @GetMapping("/{disclosureId}")
    public ResponseEntity<ApiResponse<DisclosuresResponseDTO.DisclosureDetailResponse>> getDisclosureDetail(
            @PathVariable Long disclosureId
    ) {
        DisclosuresResponseDTO.DisclosureDetailResponse result =
                disclosuresService.getDisclosureDetail(disclosureId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "기업 코드 동기화", description = "DART API에서 기업 코드를 받아 DB에 동기화")
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

    @Operation(summary = "전체 공시 수집", description = "startDate ~ endDate 기간의 전체 공시 수집 (형식: yyyyMMdd)")
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


    @Operation(summary = "기업별 공시 수집", description = "특정 기업 코드(corpCode)의 startDate ~ endDate 기간 공시 수집 (형식: yyyyMMdd)")
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

    @Operation(summary = "summaryDetail 일괄 업데이트", description = "기존 공시 중 summaryDetail 없는 건 GPT로 채움")
    @PostMapping("/fill-summary-detail")
    public ResponseEntity<ApiResponse<?>> fillSummaryDetail() {
        disclosureCollectService.fillMissingSummaryDetail();
        return ResponseEntity.accepted()
                .body(ApiResponse.success("summaryDetail 업데이트 시작됨 (백그라운드 실행 중)"));
    }

    @Operation(summary = "rawText 일괄 업데이트", description = "rawText 없는 공시를 DART에서 재수집 후 GPT 분석까지 채움 (PDF 포함)")
    @PostMapping("/fill-raw-text")
    public ResponseEntity<ApiResponse<?>> fillRawText() {
        disclosureCollectService.fillMissingRawText();
        return ResponseEntity.accepted()
                .body(ApiResponse.success("rawText 업데이트 시작됨 (백그라운드 실행 중)"));
    }
}