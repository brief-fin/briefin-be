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

        // 1. Emitter 관리 로직 (기존과 동일하되 가독성 개선)
        emitterMap.computeIfAbsent(ticker, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("SSE 연결: {} - 현재 구독자 {}명", ticker, emitterMap.get(ticker).size());

        Runnable cleanup = () -> {
            List<SseEmitter> emitters = emitterMap.get(ticker);
            if (emitters != null) {
                emitters.remove(emitter);
            }
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> { cleanup.run(); emitter.complete(); });
        emitter.onError(e -> { cleanup.run(); emitter.complete(); });

        // 2. 웹소켓 구독 요청
        try {
            lsWebSocketClient.subscribe(ticker);
        } catch (Exception e) {
            log.error("웹소켓 구독 실패: {}", ticker);
        }

        // 3. 초기 데이터(Initial Payload) 전송 로직 개선
        try {
            // [수정 포인트] DB에서 해당 기업의 기본 정보를 먼저 가져옴 (시가총액 유실 방지용)
            Companies company = companiesRepository.findByTicker(ticker).orElse(null);
            long dbMarketCap = (company != null && company.getMarketCap() != null) ? company.getMarketCap().longValue() : 0;

            StockPrice currentPrice = lsClient.getCurrentPrice(ticker);

            if (currentPrice != null && currentPrice.getCurrentPrice() > 0) {
                // [수정] set 대신 새로운 객체를 생성하여 전달
                StockPrice finalPrice = new StockPrice(
                        currentPrice.getCurrentPrice(),
                        currentPrice.getChangeRate(),
                        (currentPrice.getMarketCap() > 0) ? currentPrice.getMarketCap() : dbMarketCap
                );

                emitter.send(SseEmitter.event().data(finalPrice));
                log.info("현재가 전송: {} -> {}원", ticker, finalPrice.getCurrentPrice());
            } else {
                // 장 전/후 또는 API 실패: 전일 종가 조회
                StockPrice prevClose = lsClient.getPreviousClosePrice(ticker);

                if (prevClose != null && prevClose.getCurrentPrice() > 0) {
                    // [CodeRabbit 지적 반영] 전일 종가에 DB 시가총액 병합
                    StockPrice mergedPrice = new StockPrice(
                            prevClose.getCurrentPrice(),
                            prevClose.getChangeRate(),
                            dbMarketCap
                    );
                    emitter.send(SseEmitter.event().data(mergedPrice));
                    log.info("전일종가 전송(DB 시총 병합): {} -> {}원", ticker, prevClose.getCurrentPrice());
                } else if (company != null) {
                    // 전일 종가도 없으면 DB 데이터 최종 Fallback
                    double price = company.getCurrentPrice() != null ? company.getCurrentPrice().doubleValue() : 0.0;
                    double rate = company.getChangeRate() != null ? company.getChangeRate().doubleValue() : 0.0;
                    if (price > 0) {
                        emitter.send(SseEmitter.event().data(new StockPrice(price, rate, dbMarketCap)));
                        log.info("DB 데이터 전송: {} -> {}원", ticker, price);
                    }
                }
            }
        } catch (Exception e) {
            log.error("최초 데이터 전송 에러: {} - {}", ticker, e.getMessage());
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