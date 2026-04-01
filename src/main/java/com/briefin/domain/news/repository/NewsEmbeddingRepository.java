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
            WHERE ne.news_id != :newsId
            ORDER BY ne.embedding <=> target.embedding
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findRelatedNewsIds(@Param("newsId") Long newsId, @Param("limit") int limit);

    @Query(value = """
            SELECT id FROM (
                SELECT DISTINCT ON (DATE_TRUNC('day', n.published_at)) n.id, n.published_at,
                    ne.embedding <=> (SELECT embedding FROM news_embeddings WHERE news_id = :newsId LIMIT 1) AS dist
                FROM news n
                JOIN news_embeddings ne ON ne.news_id = n.id
                WHERE n.id != :newsId
                  AND n.published_at < (SELECT published_at FROM news WHERE id = :newsId)
                ORDER BY DATE_TRUNC('day', n.published_at), dist ASC
            ) sub
            ORDER BY dist ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findTimelineNewsIds(@Param("newsId") Long newsId, @Param("limit") int limit);
}
