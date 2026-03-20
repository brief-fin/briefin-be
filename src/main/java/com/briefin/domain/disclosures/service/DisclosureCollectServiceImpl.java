package com.briefin.domain.disclosures.service;

import com.briefin.domain.companies.entity.Companies;
import com.briefin.domain.companies.repository.CompaniesRepository;
import com.briefin.domain.corp.repository.CorpRepository;
import com.briefin.domain.disclosures.client.ChatGptClient;
import com.briefin.domain.disclosures.client.DartApiClient;
import com.briefin.domain.disclosures.dto.DisclosureItem;
import com.briefin.domain.disclosures.entity.Disclosures;
import com.briefin.domain.disclosures.repository.DisclosuresRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisclosureCollectServiceImpl implements DisclosureCollectService {  // 클래스 레벨 @Transactional 제거

    private final DartApiClient dartApiClient;
    private final ChatGptClient chatGptClient;
    private final DisclosuresRepository disclosuresRepository;
    private final CompaniesRepository companiesRepository;
    private final CorpRepository corpRepository;

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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        for (DisclosureItem item : items) {

            if (disclosuresRepository.existsByDartId(item.getRcept_no())) {
                log.debug("이미 존재하는 공시 스킵: {}", item.getRcept_no());
                continue;
            }

            Companies company = corpRepository.findByCorpCode(item.getCorp_code())
                    .flatMap(corp -> companiesRepository.findByTicker(corp.getStockCode()))
                    .orElse(null);

            if (company == null) {
                log.debug("등록되지 않은 기업 스킵: {}", item.getCorp_name());
                continue;
            }

            try {
                // 원격 호출 먼저 (트랜잭션 밖)
                String rawText = dartApiClient.fetchDisclosureText(item.getRcept_no());

                // 원문 파싱 실패면 요약 스킵
                if (rawText == null || rawText.isBlank()) {
                    log.warn("원문 파싱 실패로 요약 스킵: {}", item.getRcept_no());
                    saveDisclosure(company, item, rawText, null, formatter);
                    continue;
                }

                String summary = chatGptClient.summarize(rawText);
                // DB 저장만 트랜잭션으로

                saveDisclosure(company, item, rawText, summary, formatter);

                log.info("공시 저장 완료: {} - {}", item.getCorp_name(), item.getReport_nm());

            } catch (Exception e) {
                log.error("공시 처리 실패: {} - {}", item.getRcept_no(), e.getMessage());
            }
        }
    }

    @Transactional  // 저장 구간만 트랜잭션
    public void saveDisclosure(Companies company, DisclosureItem item,
                               String rawText, String summary, DateTimeFormatter formatter) {
        disclosuresRepository.save(Disclosures.builder()
                .company(company)
                .dartId(item.getRcept_no())
                .title(item.getReport_nm())
                .disclosedAt(LocalDate.parse(item.getRcept_dt(), formatter))
                .url("https://dart.fss.or.kr/dsaf001/main.do?rcpNo=" + item.getRcept_no())
                .rawText(rawText)
                .summary(summary)
                .build());
    }
}