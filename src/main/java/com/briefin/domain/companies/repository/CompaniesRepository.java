package com.briefin.domain.companies.repository;

import com.briefin.domain.companies.entity.Companies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompaniesRepository extends JpaRepository<Companies, Long> {

    Optional<Companies> findById(Long id);

    List<Companies> findByTickerIn(List<String> tickers);

    Page<Companies> findByNameContaining(String name, Pageable pageable);

    Optional<Companies> findByCorpCode(String corpCode);



}
