package com.briefin.domain.users.repository;

import com.briefin.domain.users.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    @Query("SELECT w FROM Watchlist w JOIN FETCH w.company WHERE w.user.id = :userId ORDER BY w.createdAt DESC")
    List<Watchlist> findByUserIdWithCompany(UUID userId);

    boolean existsByUserIdAndCompanyId(UUID userId, Long companyId);
    Optional<Watchlist> findByUserIdAndCompanyId(UUID userId, Long companyId);

    @Modifying
    @Query("DELETE FROM Watchlist w WHERE w.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);



}
