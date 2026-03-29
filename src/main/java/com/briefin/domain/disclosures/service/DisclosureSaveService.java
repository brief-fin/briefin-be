package com.briefin.domain.disclosures.service;

import com.briefin.domain.companies.entity.Companies;
import com.briefin.domain.disclosures.dto.DisclosureItem;
import com.briefin.domain.disclosures.entity.Disclosures;
import com.briefin.domain.disclosures.event.DisclosureSavedEvent;
import com.briefin.domain.disclosures.repository.DisclosuresRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisclosureSaveService {

    private final DisclosuresRepository disclosuresRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Transactional
    public void save(Companies company, DisclosureItem item, String rawText, String summary, String summaryDetail) {
        Disclosures saved = disclosuresRepository.save(Disclosures.builder()
                .company(company)
                .dartId(item.getRcept_no())
                .title(item.getReport_nm())
                .disclosedAt(LocalDate.parse(item.getRcept_dt(), FORMATTER))
                .url("https://dart.fss.or.kr/dsaf001/main.do?rcpNo=" + item.getRcept_no())
                .rawText(rawText)
                .summary(summary)
                .summaryDetail(summaryDetail)
                .category(item.getPblntf_ty())
                .build());

        applicationEventPublisher.publishEvent(
                new DisclosureSavedEvent(company.getId(), saved.getId(), company.getName(), item.getReport_nm(), item.getPblntf_ty())
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