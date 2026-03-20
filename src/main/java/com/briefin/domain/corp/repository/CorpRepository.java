package com.briefin.domain.corp.repository;

import com.briefin.domain.corp.entity.Corp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CorpRepository extends JpaRepository<Corp, Long> {
    Optional<Corp> findByStockCode(String stockCode);
    Optional<Corp> findByCorpCode(String corpCode);
    boolean existsByCorpCode(String corpCode);
}