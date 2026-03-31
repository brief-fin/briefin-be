package com.briefin.domain.news.repository;

import com.briefin.domain.news.entity.NewsView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NewsViewRepository extends JpaRepository<NewsView, Long> {

    boolean existsByUserIdAndNewsId(UUID userId, Long newsId);

    @Query(
        value = "SELECT v FROM NewsView v JOIN FETCH v.news WHERE v.userId = :userId ORDER BY v.viewedAt DESC, v.id DESC",
        countQuery = "SELECT COUNT(v) FROM NewsView v WHERE v.userId = :userId"
    )
    Page<NewsView> findByUserIdWithNews(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(v) FROM NewsView v WHERE v.userId = :userId")
    long countByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM NewsView v WHERE v.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
