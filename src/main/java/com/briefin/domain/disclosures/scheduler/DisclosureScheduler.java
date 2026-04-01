package com.briefin.domain.disclosures.scheduler;

import com.briefin.domain.disclosures.service.DisclosureCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class DisclosureScheduler {

    private final DisclosureCollectService disclosureCollectService;

    private static final DateTimeFormatter DART_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 매일 오전 6시 - 전날~당일 공시 수집 (6시 이후 공시 누락 방지, 중복은 dartId로 방어)
    @Scheduled(cron = "0 0 6 * * *")
    public void collectDailyDisclosures() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        String startDate = today.minusDays(1).format(DART_DATE_FORMAT);
        String endDate = today.format(DART_DATE_FORMAT);
        log.info("일간 공시 수집 시작: {} ~ {}", startDate, endDate);
        try {
            disclosureCollectService.collectAll(startDate, endDate);
            log.info("일간 공시 수집 완료: {} ~ {}", startDate, endDate);
        } catch (Exception e) {
            log.error("일간 공시 수집 실패: {} ~ {} - {}", startDate, endDate, e.getMessage());
        }
    }
}
