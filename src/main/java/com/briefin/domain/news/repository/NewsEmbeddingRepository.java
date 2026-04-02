package com.briefin.domain.news.repository;

import com.briefin.domain.news.entity.NewsEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsEmbeddingRepository extends JpaRepository<NewsEmbedding, Long> {

    @Query(value = """
            SELECT ne.news_id
            FROM news_embeddings ne
            JOIN news_embeddings target ON target.news_id = :newsId
            JOIN news n ON n.id = ne.news_id
            WHERE ne.news_id != :newsId
              AND n.published_at >= NOW() - INTERVAL '2 days'
            ORDER BY ne.embedding <=> target.embedding
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findRelatedNewsIds(@Param("newsId") Long newsId, @Param("limit") int limit);

    @Query(value = """
            SELECT id FROM (
                SELECT DISTINCT ON (DATE_TRUNC('week', published_at)) id, published_at, dist
                FROM (
                    SELECT n.id, n.published_at,
                        ne.embedding <=> (SELECT embedding FROM news_embeddings WHERE news_id = :newsId LIMIT 1) AS dist
                    FROM news n
                    JOIN news_embeddings ne ON ne.news_id = n.id
                    WHERE n.id != :newsId
                      AND n.published_at < (SELECT published_at FROM news WHERE id = :newsId) - INTERVAL '3 days'
                    ORDER BY dist ASC
                    LIMIT 200
                ) top_k
                ORDER BY DATE_TRUNC('week', published_at), dist ASC
            ) weekly
            ORDER BY dist ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findTimelineNewsIds(@Param("newsId") Long newsId, @Param("limit") int limit);
}
