package com.briefin.domain.news.repository;

import com.briefin.domain.news.entity.NewsView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NewsViewRepository extends JpaRepository<NewsView, Long> {

    boolean existsByUserIdAndNewsId(UUID userId, Long newsId);

    @Query("SELECT COUNT(v) FROM NewsView v WHERE v.userId = :userId")
    long countByUserId(@Param("userId") UUID userId);
}
