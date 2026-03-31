package com.briefin.domain.companies.repository;

import com.briefin.domain.companies.entity.Companies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompaniesRepository extends JpaRepository<Companies, Long> {

    Optional<Companies> findById(Long id);

    List<Companies> findByTickerIn(List<String> tickers);

    Page<Companies> findByNameContaining(String name, Pageable pageable);

    Optional<Companies> findByCorpCode(String corpCode);


    Optional<Companies> findByTicker(String ticker);

    @Modifying
    @Query("UPDATE Companies c SET c.currentPrice = :currentPrice, c.changeRate = :changeRate WHERE c.ticker = :ticker")
    void updatePrice(@Param("ticker") String ticker,
                     @Param("currentPrice") BigDecimal currentPrice,
                     @Param("changeRate") BigDecimal changeRate);

    @Modifying
    @Query("UPDATE Companies c SET c.marketCap = :marketCap WHERE c.ticker = :ticker")
    void updateMarketCap(@Param("ticker") String ticker, @Param("marketCap") BigDecimal marketCap);

    @Modifying
    @Query("UPDATE Companies c SET c.sector = :sector, c.logoUrl = :logoUrl WHERE c.ticker = :ticker")
    void updateSectorAndLogo(@Param("ticker") String ticker,
                             @Param("sector") String sector,
                             @Param("logoUrl") String logoUrl);
}
