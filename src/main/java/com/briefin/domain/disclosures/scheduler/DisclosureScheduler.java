package com.briefin.domain.disclosures.scheduler;

import com.briefin.domain.disclosures.service.DisclosureCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class DisclosureScheduler {

    private final DisclosureCollectService disclosureCollectService;

    private static final DateTimeFormatter DART_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 매일 오전 6시 - 당일 공시 수집
    @Scheduled(cron = "0 0 6 * * *")
    public void collectDailyDisclosures() {
        String today = LocalDate.now().format(DART_DATE_FORMAT);
        log.info("일간 공시 수집 시작: {}", today);
        try {
            disclosureCollectService.collectAll(today, today);
            log.info("일간 공시 수집 완료: {}", today);
        } catch (Exception e) {
            log.error("일간 공시 수집 실패: {} - {}", today, e.getMessage());
        }
    }
}
