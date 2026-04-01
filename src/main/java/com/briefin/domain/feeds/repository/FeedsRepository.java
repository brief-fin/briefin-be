package com.briefin.domain.feeds.repository;

import com.briefin.domain.news.entity.News;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeedsRepository extends JpaRepository<News, Long> {

    @Query(value = """
            SELECT DISTINCT n.id, n.published_at FROM news n
            JOIN news_companies nc ON n.id = nc.news_id
            JOIN watchlist w ON nc.company_id = w.company_id
            WHERE w.user_id = CAST(:userId AS uuid)
            ORDER BY n.published_at DESC NULLS LAST
            """,
            countQuery = """
            SELECT COUNT(DISTINCT n.id) FROM news n
            JOIN news_companies nc ON n.id = nc.news_id
            JOIN watchlist w ON nc.company_id = w.company_id
            WHERE w.user_id = CAST(:userId AS uuid)
            """,
            nativeQuery = true)
    List<Long> findWatchlistFeed(@Param("userId") UUID userId, Pageable pageable);
}
