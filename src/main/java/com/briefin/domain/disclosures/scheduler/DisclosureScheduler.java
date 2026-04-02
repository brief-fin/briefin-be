package com.briefin.domain.disclosures.scheduler;

import com.briefin.domain.disclosures.service.DisclosureCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class DisclosureScheduler {

    private final DisclosureCollectService disclosureCollectService;

    private static final DateTimeFormatter DART_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 장중(09~18시): 매 1시간 / 장외: 3시간 간격 (0, 3, 6, 9, 12, 15, 18, 21시)
    // 중복 공시는 existsByDartId로 방어 — DART API는 날짜 단위 조회만 지원
    @Scheduled(cron = "0 0 * * * *")
    public void collectDisclosures() {
        int hour = LocalTime.now(ZoneId.of("Asia/Seoul")).getHour();
        boolean isMarketHour = hour >= 9 && hour <= 18;

        if (!isMarketHour && hour % 3 != 0) {
            return;
        }

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        String startDate = today.minusDays(1).format(DART_DATE_FORMAT);
        String endDate = today.format(DART_DATE_FORMAT);

        log.info("공시 수집 시작 [{}시 / {}]: {} ~ {}", hour, isMarketHour ? "장중" : "장외", startDate, endDate);
        try {
            disclosureCollectService.collectAllThenFill(startDate, endDate);
            log.info("공시 수집 완료: {} ~ {}", startDate, endDate);
        } catch (Exception e) {
            log.error("공시 수집 실패: {} ~ {} - {}", startDate, endDate, e.getMessage());
        }
    }
}
