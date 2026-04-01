package com.briefin.domain.pushSubscription.repository;

import com.briefin.domain.pushSubscription.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, UUID> {

    // 특정 기업 구독자 전체 (공시 수집 시 푸시 발송용)
    List<PushSubscription> findByCompanyId(Long companyId);

    // 유저가 특정 기업 이미 구독 중인지 확인
    boolean existsByUserIdAndCompanyIdAndEndpoint(UUID userId, Long companyId, String endpoint);

    // 구독 취소
    void deleteByUserIdAndCompanyId(UUID userId, Long companyId);

    boolean existsByUserIdAndCompanyId(UUID userId, Long companyId);

    // 유저가 구독 중인 기업 ID 목록 (중복 제거)
    @Query("SELECT DISTINCT p.companyId FROM PushSubscription p WHERE p.userId = :userId")
    List<Long> findDistinctCompanyIdsByUserId(@Param("userId") UUID userId);
}