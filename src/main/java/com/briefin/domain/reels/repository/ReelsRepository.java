package com.briefin.domain.reels.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.briefin.domain.news.entity.News;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReelsRepository extends JpaRepository<News, Long> {

    // 관심기업 + 스크랩 + 최근 본 뉴스 임베딩 평균 기반 개인화 피드
    // 스크랩이 없으면 관심기업 + 조회 이력으로만 벡터 생성 (자동 처리)
    @Query(value = """
            WITH source_news AS (
                SELECT news_id FROM (
                    SELECT news_id FROM scraps WHERE user_id = CAST(:userId AS uuid)
                    ORDER BY created_at DESC LIMIT 20
                ) s
                UNION ALL
                SELECT news_id FROM (
                    SELECT news_id FROM news_views WHERE user_id = CAST(:userId AS uuid)
                    ORDER BY viewed_at DESC LIMIT 10
                ) v
                UNION ALL
                SELECT news_id FROM (
                    SELECT nc.news_id
                    FROM news_companies nc
                    JOIN watchlist w ON w.company_id = nc.company_id
                    JOIN news n ON n.id = nc.news_id
                    WHERE w.user_id = CAST(:userId AS uuid)
                    GROUP BY nc.news_id, n.published_at
                    ORDER BY n.published_at DESC NULLS LAST
                    LIMIT 10
                ) wl
            ),
            user_vector AS (
                SELECT AVG(ne.embedding) AS vec
                FROM news_embeddings ne
                WHERE ne.news_id IN (SELECT news_id FROM source_news)
            ),
            excluded_ids AS (
                SELECT news_id FROM scraps WHERE user_id = CAST(:userId AS uuid)
                UNION
                SELECT news_id FROM news_views WHERE user_id = CAST(:userId AS uuid)
            )
            SELECT ne.news_id
            FROM news_embeddings ne
            CROSS JOIN user_vector
            WHERE ne.news_id NOT IN (SELECT news_id FROM excluded_ids)
              AND user_vector.vec IS NOT NULL
            ORDER BY ne.embedding <=> user_vector.vec
            LIMIT 20
            """, nativeQuery = true)
    List<Long> findPersonalizedFeed(@Param("userId") UUID userId);

    // Cold start 폴백: 최신 뉴스 (본 뉴스 + 스크랩 제외)
    @Query(value = """
            SELECT n.id FROM news n
            WHERE n.id NOT IN (
                SELECT news_id FROM scraps WHERE user_id = CAST(:userId AS uuid)
                UNION
                SELECT news_id FROM news_views WHERE user_id = CAST(:userId AS uuid)
            )
            ORDER BY n.published_at DESC NULLS LAST
            LIMIT 20
            """, nativeQuery = true)
    List<Long> findFallbackFeed(@Param("userId") UUID userId);
}
