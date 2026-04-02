package com.briefin.domain.disclosures.repository;

import com.briefin.domain.disclosures.entity.Disclosures;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DisclosuresRepository extends JpaRepository<Disclosures, Long> {

    // 1. 기업ID로 목록 조회 (페이징)
    Page<Disclosures> findByCompanyId(Long companyId, Pageable pageable);

    // 1. 전체 목록 조회 (페이징)
    Page<Disclosures> findAll(Pageable pageable);

    // 3. 기업별 최근 공시 3개
    List<Disclosures> findTop3ByCompanyIdOrderByDisclosedAtDesc(Long companyId);

    boolean existsByDartId(String dartId);

    List<Disclosures> findTop30ByCompanyIdOrderByDisclosedAtDesc(Long companyId);

    List<Disclosures> findBySummaryDetailIsNull();

    @org.springframework.data.jpa.repository.Query("SELECT d FROM Disclosures d WHERE d.rawText IS NULL OR d.rawText = ''")
    List<Disclosures> findByRawTextNullOrEmpty();
}