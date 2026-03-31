package com.briefin.domain.pushSubscription.listener;

import com.briefin.domain.disclosures.event.DisclosureSavedEvent;
import com.briefin.domain.pushSubscription.service.WebPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DisclosureSavedEventListener {

    private final WebPushService webPushService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDisclosureSaved(DisclosureSavedEvent event) {
        try {
            webPushService.sendToSubscribers(
                    event.companyId(),
                    event.disclosureId(),
                    event.companyName() + " 새 공시",
                    event.reportName()
            );
        } catch (Exception e) {
            log.error("푸시 발송 실패 - companyId={}, error={}", event.companyId(), e.getMessage(), e);
        }
    }
}