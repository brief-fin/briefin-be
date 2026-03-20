package com.briefin.domain.companies.repository;

import com.briefin.domain.companies.entity.Companies;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompaniesRepository extends JpaRepository<Companies, Long> {

    Optional<Companies> findById(Long id);


    List<Companies> findByTickerIn(List<String> tickers);

    Optional<Companies> findByTicker(String ticker);
    Optional<Companies> findByStockCode(String stockCode);
    Optional<Companies> findByCorpCode(String corpCode);
    boolean existsByCorpCode(String corpCode);




}
