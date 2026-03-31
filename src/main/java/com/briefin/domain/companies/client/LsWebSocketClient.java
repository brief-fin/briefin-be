package com.briefin.domain.companies.client;

import com.briefin.domain.companies.entity.StockPrice;
import com.briefin.domain.companies.event.StockPriceUpdatedEvent;
import com.briefin.domain.companies.manager.LsTokenManager;
import com.briefin.domain.companies.socket.StockPriceCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor
@Slf4j
public class LsWebSocketClient {

    private final LsTokenManager lsTokenManager;


    private final StockPriceCache stockPriceCache;
    private final ApplicationEventPublisher eventPublisher;
    private WebSocketSession session;

    @PostConstruct
    public void connect() {
        try {
            WebSocketClient client = new StandardWebSocketClient();
            client.execute(new AbstractWebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                    LsWebSocketClient.this.session = session;
                    log.info("LS증권 웹소켓 연결 완료");
                }

                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                    try {
                        String payload = message.getPayload();
                        log.debug("수신: {}", payload);
                        parseAndCache(payload);
                    } catch (Exception e) {
                        log.error("메시지 처리 실패", e);
                    }
                }

                @Override
                public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                    log.warn("LS증권 웹소켓 연결 종료: {}", status);
                    reconnect();
                }
            }, "wss://openapi.ls-sec.co.kr:9443/websocket").get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("웹소켓 연결 중단", e);
        } catch (ExecutionException e) {
            log.error("웹소켓 연결 실패", e);
        }
    }

    public void unsubscribe(String ticker) {
        if (session == null || !session.isOpen()) return;
        try {
            String token = lsTokenManager.getToken();
            String msg = String.format("""
                    {
                        "header": {
                            "token": "%s",
                            "tr_type": "4"
                        },
                        "body": {
                            "tr_cd": "S3_",
                            "tr_key": "%s"
                        }
                    }
                    """, token, ticker);
            session.sendMessage(new TextMessage(msg));
            log.info("구독 해제: {}", ticker);
        } catch (Exception e) {
            log.error("구독 해제 실패: {}", ticker);
        }
    }

    public void subscribe(String ticker) throws Exception {
        int retry = 0;
        String token = lsTokenManager.getToken();
        while ((session == null || !session.isOpen()) && retry < 10) {
            log.info("웹소켓 연결 대기중... {}", retry);
            Thread.sleep(500);
            retry++;
        }

        if (session == null || !session.isOpen()) {
            log.warn("웹소켓 연결 안됨 - 구독 실패: {}", ticker);
            return;
        }

        String trCd = "S3_";
        String subscribeMsg = String.format("""
                {
                    "header": {
                        "token": "%s",
                        "tr_type": "3"
                    },
                    "body": {
                        "tr_cd": "%s",
                        "tr_key": "%s"
                    }
                }
                """, token, trCd, ticker);

        session.sendMessage(new TextMessage(subscribeMsg));
        log.info("구독 요청: {} ({})", ticker, trCd);
    }

    private void parseAndCache(String payload) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(payload);
            String ticker = node.path("header").path("tr_key").asText();
            String priceStr = node.path("body").path("price").asText();
            String diffStr = node.path("body").path("drate").asText();


           // log.info("파싱 중: ticker={}, price={}, drate={}", ticker, priceStr, diffStr); // ← 추가


            if (ticker.isEmpty() || priceStr.isEmpty()) return;

            double price = Double.parseDouble(priceStr);
            double diff = diffStr.isEmpty() ? 0.0 : Double.parseDouble(diffStr);


            if (!ticker.isEmpty() && price > 0) {
                // 이전 값과 비교
                StockPrice previous = stockPriceCache.get(ticker);

                if (previous == null || previous.getCurrentPrice() != price) {
                    // 변동 있을 때만 업데이트 & 이벤트 발행
//                    log.info("가격 변동 감지: {} {} → {}원 ({}%)",
//                            ticker, previous != null ? previous.getCurrentPrice() : "없음", price, diff);
                    StockPrice stockPrice = new StockPrice(price, diff, 0L);
                    stockPriceCache.update(ticker, stockPrice);
                    eventPublisher.publishEvent(new StockPriceUpdatedEvent(ticker, price, diff, 0L));
                    log.debug("가격 변동: {} → {}원 ({}%)", ticker, price, diff);
                }
            }
        } catch (Exception e) {
            //log.error("파싱 실패 상세: {} - {}", e.getMessage(), payload); // ← 상세 에러 추가
        }
    }

    private void reconnect() {
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                connect();
            } catch (Exception e) {
                log.error("재연결 실패", e);
            }
        }).start();
    }
}