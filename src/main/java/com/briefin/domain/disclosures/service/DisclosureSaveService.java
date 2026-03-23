package com.briefin.domain.disclosures.service;

import com.briefin.domain.companies.entity.Companies;
import com.briefin.domain.disclosures.dto.DisclosureItem;
import com.briefin.domain.disclosures.entity.Disclosures;
import com.briefin.domain.disclosures.repository.DisclosuresRepository;
import com.briefin.domain.pushSubscription.service.WebPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisclosureSaveService {

    private final DisclosuresRepository disclosuresRepository;
    private final WebPushService webPushService;  // 추가

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Transactional
    public void save(Companies company, DisclosureItem item, String rawText, String summary, String summaryDetail) {
        disclosuresRepository.save(Disclosures.builder()
                .company(company)
                .dartId(item.getRcept_no())
                .title(item.getReport_nm())
                .disclosedAt(LocalDate.parse(item.getRcept_dt(), FORMATTER))
                .url("https://dart.fss.or.kr/dsaf001/main.do?rcpNo=" + item.getRcept_no())
                .rawText(rawText)
                .summary(summary)
                .summaryDetail(summaryDetail)
                .build());

        // 공시 저장 후 구독자에게 푸시 발송
        webPushService.sendToSubscribers(
                company.getId(),
                company.getName() + " 새 공시",
                item.getReport_nm()
        );
    }

    @Transactional
    public void updateSummaryDetail(Long id, String summaryDetail) {
        disclosuresRepository.findById(id).ifPresentOrElse(
                d -> d.updateSummaryDetail(summaryDetail),
                () -> log.warn("summaryDetail 업데이트 대상 공시 미존재: id={}", id)
        );
    }
}