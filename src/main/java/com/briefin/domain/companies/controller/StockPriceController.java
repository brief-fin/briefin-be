package com.briefin.domain.companies.controller;

import com.briefin.domain.companies.client.LsClient;
import com.briefin.domain.companies.client.LsWebSocketClient;
import com.briefin.domain.companies.entity.StockPrice;
import com.briefin.domain.companies.event.StockPriceUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/companies")
@Slf4j
public class StockPriceController {

    private final Map<String, List<SseEmitter>> emitterMap = new ConcurrentHashMap<>();
    private final LsClient lsClient;
    private final LsWebSocketClient lsWebSocketClient;

    @GetMapping(value = "/{ticker}/price", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPrice(@PathVariable String ticker) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitterMap.computeIfAbsent(ticker, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("SSE 연결: {} - 현재 구독자 {}명", ticker, emitterMap.get(ticker).size()); // ← 추가

        emitter.onCompletion(() -> {
            emitterMap.get(ticker).remove(emitter);
            log.info("SSE 연결 종료: {}", ticker); // ← 추가
        });
        emitter.onTimeout(() -> {
            emitterMap.get(ticker).remove(emitter);
            emitter.complete();
            log.info("SSE 타임아웃: {}", ticker); // ← 추가
        });
        emitter.onError(e -> {
            emitterMap.get(ticker).remove(emitter);
            emitter.complete();
            log.error("SSE 에러: {}", e.getMessage()); // ← 추가
        });

        try {
            lsWebSocketClient.subscribe(ticker);
        } catch (Exception e) {
            log.error("구독 실패: {}", ticker);
        }

        try {
            StockPrice initialPrice = lsClient.getCurrentPrice(ticker);
            if (initialPrice != null) {
                emitter.send(SseEmitter.event().data(initialPrice));
            }
        } catch (Exception e) {
            log.error("최초 조회 실패: {}", ticker);
        }

        return emitter;
    }

    @EventListener
    public void onStockPriceUpdate(StockPriceUpdatedEvent event) {
        List<SseEmitter> emitters = emitterMap.get(event.ticker());
        if (emitters == null || emitters.isEmpty()) return;

        log.info("SSE 전송: {} → {}원 ({}%) - 구독자 {}명",
                event.ticker(), event.price(), event.diff(), emitters.size()); // ← 추가

        for (SseEmitter emitter : new CopyOnWriteArrayList<>(emitters)) {
            try {
                emitter.send(SseEmitter.event()
                        .name("realtime-price")
                        .data(new StockPrice(event.price(), event.diff())));
            } catch (Exception e) {
                log.error("SSE 전송 실패: {}", e.getMessage());
                emitter.complete();
                emitters.remove(emitter);
            }
        }
    }


}