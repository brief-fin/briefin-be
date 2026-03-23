package com.briefin.domain.pushSubscription.service;

import com.briefin.domain.pushSubscription.entity.PushSubscription;
import com.briefin.domain.pushSubscription.repository.PushSubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebPushServiceImpl implements WebPushService {

    @Value("${vapid.public-key}")
    private String vapidPublicKey;

    @Value("${vapid.private-key}")
    private String vapidPrivateKey;

    @Value("${vapid.subject}")
    private String vapidSubject;

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final ObjectMapper objectMapper;
    private PushService pushService;

    @PostConstruct
    public void init() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
    }

    @Override
    @Transactional
    public void subscribe(UUID userId, UUID companyId, String endpoint, String p256dh, String auth) {
        if (pushSubscriptionRepository.existsByUserIdAndCompanyIdAndEndpoint(userId, companyId, endpoint)) {
            log.debug("이미 구독 중: userId={}, companyId={}", userId, companyId);
            return;
        }
        pushSubscriptionRepository.save(PushSubscription.builder()
                .userId(userId)
                .companyId(companyId)
                .endpoint(endpoint)
                .p256dh(p256dh)
                .auth(auth)
                .build());
        log.info("푸시 구독 저장: userId={}, companyId={}", userId, companyId);
    }

    @Override
    @Transactional
    public void unsubscribe(UUID userId, UUID companyId) {
        pushSubscriptionRepository.deleteByUserIdAndCompanyId(userId, companyId);
        log.info("푸시 구독 취소: userId={}, companyId={}", userId, companyId);
    }

    @Override
    public void sendToSubscribers(UUID companyId, String title, String body) {
        List<PushSubscription> subscribers = pushSubscriptionRepository.findByCompanyId(companyId);
        if (subscribers.isEmpty()) return;

        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                    "title", title,
                    "body", body
            ));
        } catch (Exception e) {
            log.error("푸시 payload 직렬화 실패: {}", e.getMessage());
            return;
        }

        for (PushSubscription sub : subscribers) {
            try {
                Notification notification = new Notification(
                        sub.getEndpoint(),
                        sub.getP256dh(),
                        sub.getAuth(),
                        payload
                );
                pushService.send(notification);
                log.info("푸시 발송 완료: companyId={}", companyId);
            } catch (Exception e) {
                log.error("푸시 발송 실패: endpoint={}, error={}", sub.getEndpoint(), e.getMessage());
            }
        }
    }
}