package com.briefin.domain.pushSubscription.dto;

import lombok.Getter;
import java.util.UUID;

public class PushSubscriptionRequestDTO {

    @Getter
    public static class SubscribeRequest {
        private UUID companyId;
        private String endpoint;
        private String p256dh;
        private String auth;
    }

    @Getter
    public static class UnsubscribeRequest {
        private UUID companyId;
    }
}