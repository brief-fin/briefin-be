package com.briefin.domain.users.repository;

import com.briefin.domain.users.entity.Scraps;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ScrapsRepository extends JpaRepository<Scraps, Long> {

    @Query("SELECT s FROM Scraps s JOIN FETCH s.news WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    Page<Scraps> findByUserIdWithNews(UUID userId, Pageable pageable);

    boolean existsByUserIdAndNewsId(UUID userId, Long newsId);

    java.util.Optional<Scraps> findByUserIdAndNewsId(UUID userId, Long newsId);
}
