package com.briefin.domain.disclosures.service;

import com.briefin.domain.companies.entity.Companies;
import com.briefin.domain.companies.repository.CompaniesRepository;
import com.briefin.domain.disclosures.client.ChatGptClient;
import com.briefin.domain.disclosures.client.DartApiClient;
import com.briefin.domain.disclosures.dto.DisclosureItem;
import com.briefin.domain.disclosures.entity.Disclosures;
import com.briefin.domain.disclosures.repository.DisclosuresRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisclosureCollectServiceImpl implements DisclosureCollectService {

    private final DartApiClient dartApiClient;
    private final ChatGptClient chatGptClient;
    private final DisclosuresRepository disclosuresRepository;
    private final CompaniesRepository companiesRepository;
    private final DisclosureSaveService disclosureSaveService;

    @Override
    public void syncCorpCodes() throws Exception {
        dartApiClient.syncCorpCodes();
        log.info("corp_code 동기화 완료");
    }

    @Async("disclosureCollectExecutor")
    @Override
    public void collectAll(String startDate, String endDate) {
        List<DisclosureItem> items = dartApiClient.fetchAllDisclosures(startDate, endDate);
        log.info("전체 공시 수집 시작: {}건", items.size());
        saveDisclosures(items);
    }

    @Async("disclosureCollectExecutor")
    @Override
    public void collectAllThenFill(String startDate, String endDate) {
        List<DisclosureItem> items = dartApiClient.fetchAllDisclosures(startDate, endDate);
        log.info("전체 공시 수집 시작: {}건", items.size());
        saveDisclosures(items);
        log.info("수집 완료 — rawText 보완 시작");
        fillMissingRawTextInternal();
    }

    @Async("disclosureCollectExecutor")
    @Override
    public void collectByCorpCode(String corpCode, String startDate, String endDate) {
        List<DisclosureItem> items = dartApiClient.fetchDisclosuresByCorpCode(corpCode, startDate, endDate);
        log.info("기업별 공시 수집 시작 - corpCode: {}, {}건", corpCode, items.size());
        saveDisclosures(items);
    }

    private void saveDisclosures(List<DisclosureItem> items) {
        for (DisclosureItem item : items) {
            if (item.getReport_nm() != null && item.getReport_nm().startsWith("[첨부정정]")) {
                log.debug("첨부정정 공시 스킵: {}", item.getReport_nm());
                continue;
            }

            if (disclosuresRepository.existsByDartId(item.getRcept_no())) {
                log.debug("이미 존재하는 공시 스킵: {}", item.getRcept_no());
                continue;
            }

            Companies company = companiesRepository.findByCorpCode(item.getCorp_code()).orElse(null);
            if (company == null) {
                log.debug("등록되지 않은 기업 스킵: {}", item.getCorp_name());
                continue;
            }

            try {
                String rawText = dartApiClient.fetchDisclosureText(item.getRcept_no());
                if (rawText == null || rawText.isBlank()) {
                    log.warn("원문 추출 실패로 저장 보류: {} - {}", item.getRcept_no(), item.getReport_nm());
                    continue;
                }

                String summary = chatGptClient.summarize(rawText);
                String summaryDetail = safeSummarizeDetail(item.getRcept_no(), rawText);

                disclosureSaveService.save(company, item, rawText, summary, summaryDetail);
                log.info("공시 저장 완료: {} - {}", item.getCorp_name(), item.getReport_nm());

            } catch (DataIntegrityViolationException e) {
                log.debug("중복 공시 스킵 (race condition): {}", item.getRcept_no());
            } catch (Exception e) {
                log.error("공시 처리 실패: {} - {}", item.getRcept_no(), e.getMessage());
            }
        }
    }

    @Async
    @Override
    public void fillMissingRawText() {
        fillMissingRawTextInternal();
    }

    private void fillMissingRawTextInternal() {
        List<Disclosures> targets = disclosuresRepository.findByRawTextNullOrEmpty();
        log.info("rawText 미설정 공시: {}건", targets.size());

        for (Disclosures disclosure : targets) {
            try {
                String rawText = dartApiClient.fetchDisclosureText(disclosure.getDartId());
                if (rawText == null || rawText.isBlank()) {
                    log.warn("rawText 추출 실패 (빈 결과): {}", disclosure.getDartId());
                    continue;
                }

                String summary = chatGptClient.summarize(rawText);
                String summaryDetail = safeSummarizeDetail(disclosure.getDartId(), rawText);
                disclosureSaveService.updateRawTextAndSummaries(disclosure.getId(), rawText, summary, summaryDetail);
                log.info("rawText 업데이트 완료: {}", disclosure.getDartId());
            } catch (Exception e) {
                log.error("rawText 업데이트 실패: {} - {}", disclosure.getDartId(), e.getMessage());
            }
        }
    }

    @Async
    @Override
    public void fillMissingSummaryDetail() {
        List<Disclosures> targets = disclosuresRepository.findBySummaryDetailIsNull();
        log.info("summaryDetail 미설정 공시: {}건", targets.size());

        for (Disclosures disclosure : targets) {
            try {
                String summaryDetail = chatGptClient.summarizeDetail(disclosure.getRawText());
                disclosureSaveService.updateSummaryDetail(disclosure.getId(), summaryDetail);
                log.info("summaryDetail 업데이트 완료: {}", disclosure.getDartId());
            } catch (Exception e) {
                log.error("summaryDetail 업데이트 실패: {} - {}", disclosure.getDartId(), e.getMessage());
            }
        }
    }

    private String safeSummarizeDetail(String dartId, String rawText) {
        try {
            return chatGptClient.summarizeDetail(rawText);
        } catch (Exception e) {
            log.warn("상세 요약 실패, 기본 요약만 유지: {} - {}", dartId, e.getMessage());
            return null;
        }
    }
}
