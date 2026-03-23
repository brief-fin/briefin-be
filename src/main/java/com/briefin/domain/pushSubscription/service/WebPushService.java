package com.briefin.domain.pushSubscription.service;

import java.util.UUID;

public interface WebPushService {
    void subscribe(UUID userId, UUID companyId, String endpoint, String p256dh, String auth);
    void unsubscribe(UUID userId, UUID companyId);
    void sendToSubscribers(UUID companyId, String title, String body);
}