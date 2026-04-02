package com.briefin.domain.pushSubscription.service;

import com.briefin.domain.pushSubscription.dto.SubscribedCompanyResponse;

import java.util.List;
import java.util.UUID;

public interface WebPushService {
    void subscribe(UUID userId, Long companyId, String endpoint, String p256dh, String auth);
    void unsubscribe(UUID userId, Long companyId);
    void sendToSubscribers(Long companyId, Long disclosureId, String title, String body);
    boolean isSubscribed(UUID userId, Long companyId);
    List<SubscribedCompanyResponse> getSubscribedCompanies(UUID userId);
}