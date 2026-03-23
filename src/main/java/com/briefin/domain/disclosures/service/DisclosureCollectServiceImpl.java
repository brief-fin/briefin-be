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
    private final DisclosureSaveService disclosureSaveService; // 추가

    @Override
    public void syncCorpCodes() throws Exception {
        dartApiClient.syncCorpCodes();
        log.info("corp_code 동기화 완료");
    }

    @Override
    public void collectAll(String startDate, String endDate) {
        List<DisclosureItem> items = dartApiClient.fetchAllDisclosures(startDate, endDate);
        log.info("전체 공시 수집 시작: {}건", items.size());
        saveDisclosures(items);
    }

    @Override
    public void collectByCorpCode(String corpCode, String startDate, String endDate) {
        List<DisclosureItem> items = dartApiClient.fetchDisclosuresByCorpCode(corpCode, startDate, endDate);
        log.info("기업별 공시 수집 시작 - corpCode: {}, {}건", corpCode, items.size());
        saveDisclosures(items);
    }

    private void saveDisclosures(List<DisclosureItem> items) {
        for (DisclosureItem item : items) {

            // 최적화용 선체크 (TOCTOU 완전 방지는 아님 — catch로 보완)
            if (disclosuresRepository.existsByDartId(item.getRcept_no())) {
                log.debug("이미 존재하는 공시 스킵: {}", item.getRcept_no());
                continue;
            }

            Companies company = companiesRepository.findByCorpCode(item.getCorp_code())
                    .orElse(null);

            if (company == null) {
                log.debug("등록되지 않은 기업 스킵: {}", item.getCorp_name());
                continue;
            }

            try {
                // 원격 호출 (트랜잭션 밖)
                String rawText = dartApiClient.fetchDisclosureText(item.getRcept_no());
                String summary = chatGptClient.summarize(rawText);
                String summaryDetail = chatGptClient.summarizeDetail(rawText);

                log.info("summaryDetail: {}", summaryDetail);

                // 별도 서비스로 트랜잭션 보장
                disclosureSaveService.save(company, item, rawText, summary, summaryDetail);

                log.info("공시 저장 완료: {} - {}", item.getCorp_name(), item.getReport_nm());

            } catch (DataIntegrityViolationException e) {
                // 동시 실행으로 인한 중복 저장 시도 — 정상 케이스로 처리
                log.debug("중복 공시 스킵 (race condition): {}", item.getRcept_no());
            } catch (Exception e) {
                log.error("공시 처리 실패: {} - {}", item.getRcept_no(), e.getMessage());
            }
        }
    }

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
}