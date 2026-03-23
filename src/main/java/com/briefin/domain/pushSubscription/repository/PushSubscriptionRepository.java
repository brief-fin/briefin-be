package com.briefin.domain.pushSubscription.repository;

import com.briefin.domain.pushSubscription.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, UUID> {

    // 특정 기업 구독자 전체 (공시 수집 시 푸시 발송용)
    List<PushSubscription> findByCompanyId(UUID companyId);

    // 유저가 특정 기업 이미 구독 중인지 확인
    boolean existsByUserIdAndCompanyIdAndEndpoint(UUID userId, UUID companyId, String endpoint);

    // 구독 취소
    void deleteByUserIdAndCompanyId(UUID userId, UUID companyId);

    // 유저 구독 목록 조회
    List<PushSubscription> findByUserId(UUID userId);
}