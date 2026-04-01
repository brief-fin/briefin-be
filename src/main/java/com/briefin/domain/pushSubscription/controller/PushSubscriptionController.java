package com.briefin.domain.pushSubscription.controller;

import com.briefin.domain.pushSubscription.dto.PushSubscriptionRequestDTO;
import com.briefin.domain.pushSubscription.dto.SubscribedCompanyResponse;
import com.briefin.domain.pushSubscription.service.WebPushService;
import com.briefin.global.apipayload.ApiResponse;
import com.briefin.global.security.jwt.JwtUserInfo;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushSubscriptionController {

    @Value("${vapid.public-key}")
    private String vapidPublicKey;

    private final WebPushService webPushService;

    @Operation(summary = "VAPID 공개키 조회", description = "프론트에서 구독 시 필요한 공개키 반환")
    @GetMapping("/vapid-public-key")
    public ResponseEntity<ApiResponse<String>> getVapidPublicKey() {
        return ResponseEntity.ok(ApiResponse.success(vapidPublicKey));
    }

    @Operation(summary = "푸시 구독", description = "특정 기업 공시 알림 구독 (로그인 필요)")
    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<?>> subscribe(
            @Valid @RequestBody PushSubscriptionRequestDTO.SubscribeRequest request,
            @AuthenticationPrincipal JwtUserInfo jwtUserInfo
    ) {
        UUID userId = jwtUserInfo.userId();
        webPushService.subscribe(
                userId,
                request.getCompanyId(),
                request.getEndpoint(),
                request.getP256dh(),
                request.getAuth()
        );
        return ResponseEntity.ok(ApiResponse.success("구독 완료"));
    }

    @Operation(summary = "푸시 구독 취소", description = "특정 기업 공시 알림 구독 취소 (로그인 필요)")
    @DeleteMapping("/unsubscribe")
    public ResponseEntity<ApiResponse<?>> unsubscribe(
            @RequestParam Long companyId,
            @AuthenticationPrincipal JwtUserInfo jwtUserInfo
    ) {
        UUID userId = jwtUserInfo.userId();
        webPushService.unsubscribe(userId, companyId);
        return ResponseEntity.ok(ApiResponse.success("구독 취소 완료"));
    }

    @Operation(summary = "구독 기업 목록 조회", description = "로그인 유저가 알림 신청한 기업 목록 반환 (기업 ID, 이름, 로고)")
    @GetMapping("/subscriptions")
    public ResponseEntity<ApiResponse<List<SubscribedCompanyResponse>>> getSubscribedCompanies(
            @AuthenticationPrincipal JwtUserInfo jwtUserInfo
    ) {
        List<SubscribedCompanyResponse> result = webPushService.getSubscribedCompanies(jwtUserInfo.userId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "푸시 구독 여부 조회", description = "특정 기업 공시 알림 구독 여부 조회")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Boolean>> getSubscriptionStatus(
            @RequestParam Long companyId,
            @AuthenticationPrincipal JwtUserInfo jwtUserInfo
    ) {
        UUID userId = jwtUserInfo.userId();
        boolean subscribed = webPushService.isSubscribed(userId, companyId);
        return ResponseEntity.ok(ApiResponse.success(subscribed));
    }
}