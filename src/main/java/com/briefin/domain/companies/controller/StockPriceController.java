package com.briefin.domain.companies.controller;

import com.briefin.domain.companies.client.LsClient;
import com.briefin.domain.companies.client.LsWebSocketClient;
import com.briefin.domain.companies.dto.PopularCompanyDto;
import com.briefin.domain.companies.dto.PopularCompanyResponseDto;
import com.briefin.domain.companies.entity.Companies;
import com.briefin.domain.companies.entity.StockPrice;
import com.briefin.domain.companies.event.StockPriceUpdatedEvent;
import com.briefin.domain.companies.repository.CompaniesRepository;
import com.briefin.domain.companies.socket.StockPriceCache;
import com.briefin.global.apipayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/companies")
@Slf4j
public class StockPriceController {

    private final Map<String, List<SseEmitter>> emitterMap = new ConcurrentHashMap<>();
    private final LsClient lsClient;
    private final LsWebSocketClient lsWebSocketClient;
    private final StockPriceCache stockPriceCache;
    private final CompaniesRepository companiesRepository;

    @GetMapping(value = "/{ticker}/price", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPrice(@PathVariable String ticker) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        final SseEmitter finalEmitter = emitter;

        emitterMap.computeIfAbsent(ticker, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("SSE 연결: {} - 현재 구독자 {}명", ticker, emitterMap.get(ticker).size());

        emitter.onCompletion(() -> {
            emitterMap.get(ticker).remove(emitter);
            log.info("SSE 연결 종료: {}", ticker);
        });
        emitter.onTimeout(() -> {
            emitterMap.get(ticker).remove(emitter);
            emitter.complete();
            log.info("SSE 타임아웃: {}", ticker);
        });
        emitter.onError(e -> {
            emitterMap.get(ticker).remove(emitter);
            emitter.complete();
            log.error("SSE 에러: {}", e.getMessage());
        });

        try {
            lsWebSocketClient.subscribe(ticker);
        } catch (Exception e) {
            log.error("구독 실패: {}", ticker);
        }

        try {
            StockPrice initialPrice = lsClient.getCurrentPrice(ticker);

            if (initialPrice == null || initialPrice.getCurrentPrice() == 0) {
                // 장 전/마감 후 → 전일 종가 조회
                StockPrice prevClose = lsClient.getPreviousClosePrice(ticker);
                if (prevClose != null && prevClose.getCurrentPrice() > 0) {
                    finalEmitter.send(SseEmitter.event().data(prevClose));
                    log.info("전일종가 전송: {} → {}원", ticker, prevClose.getCurrentPrice());
                } else {
                    // 전일 종가도 없으면 DB에서 가져오기
                    companiesRepository.findByTicker(ticker).ifPresent((Companies company) -> {
                        try {
                            double price = company.getCurrentPrice() != null ? company.getCurrentPrice().doubleValue() : 0.0;
                            double rate = company.getChangeRate() != null ? company.getChangeRate().doubleValue() : 0.0;
                            long marketCap = company.getMarketCap() != null ? company.getMarketCap().longValue() : 0;
                            if (price > 0) {
                                finalEmitter.send(SseEmitter.event().data(new StockPrice(price, rate, marketCap)));
                                log.info("DB 현재가 전송: {} → {}원", ticker, price);
                            }
                        } catch (Exception ex) {
                            log.error("DB 현재가 전송 실패: {}", ticker);
                        }
                    });
                }
            } else {
                emitter.send(SseEmitter.event().data(initialPrice));
                log.info("현재가 전송: {} → {}원", ticker, initialPrice.getCurrentPrice());
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
                        .data(new StockPrice(event.price(), event.diff(),event.marketPrice())));
            } catch (Exception e) {
                log.error("SSE 전송 실패: {}", e.getMessage());
                emitter.complete();
                emitters.remove(emitter);
            }
        }
    }

    @GetMapping("/popular/diff")
    public ApiResponse<List<PopularCompanyResponseDto>> getTopDiffCompanies() {
        return ApiResponse.success(buildPopularResponse(lsClient.getTopDiffTickers()));
    }

    @GetMapping("/popular/market-cap")
    public ApiResponse<List<PopularCompanyResponseDto>> getTopMarketCapCompanies() {
        return ApiResponse.success(buildPopularResponse(lsClient.getTopMarketCapTickers()));
    }

    @GetMapping("/popular/volume")
    public ApiResponse<List<PopularCompanyResponseDto>> getTopVolumeCompanies() {
        return ApiResponse.success(buildPopularResponse(lsClient.getTopVolumeTickers()));
    }

    @GetMapping("/popular/value")
    public ApiResponse<List<PopularCompanyResponseDto>> getTopValueCompanies() {
        return ApiResponse.success(buildPopularResponse(lsClient.getTopValueTickers()));
    }

    // 공통 메서드
    private List<PopularCompanyResponseDto> buildPopularResponse(List<PopularCompanyDto> popularList) {
        List<String> tickers = popularList.stream()
                .map(PopularCompanyDto::getTicker)
                .collect(Collectors.toList());

        List<Companies> companies = companiesRepository.findByTickerIn(tickers);

        return companies.stream()
                .map(company -> {
                    double diff = popularList.stream()
                            .filter(p -> p.getTicker().equals(company.getTicker()))
                            .map(PopularCompanyDto::getDiff)
                            .findFirst()
                            .orElse(0.0);
                    return PopularCompanyResponseDto.of(company, diff);
                })
                .collect(Collectors.toList());
    }

}