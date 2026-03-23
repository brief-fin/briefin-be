package com.briefin.domain.disclosures.service;

import com.briefin.domain.companies.entity.Companies;
import com.briefin.domain.disclosures.dto.DisclosureItem;
import com.briefin.domain.disclosures.entity.Disclosures;
import com.briefin.domain.disclosures.repository.DisclosuresRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class DisclosureSaveService {

    private final DisclosuresRepository disclosuresRepository;

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
    }

    @Transactional
    public void updateSummaryDetail(Long id, String summaryDetail) {
        disclosuresRepository.findById(id)
                .ifPresent(d -> d.updateSummaryDetail(summaryDetail));
    }
}