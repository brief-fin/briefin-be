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
            SELECT DISTINCT n.id, n.published_at
            FROM news n
            JOIN news_embeddings ne ON ne.news_id = n.id
            JOIN news_companies nc ON nc.news_id = n.id
            WHERE n.id != :newsId
              AND nc.company_id IN (
                  SELECT company_id FROM news_companies WHERE news_id = :newsId
              )
              AND ne.embedding <=> (
                  SELECT embedding FROM news_embeddings WHERE news_id = :newsId LIMIT 1
              ) <= 0.45
            ORDER BY n.published_at ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findTimelineNewsIds(@Param("newsId") Long newsId, @Param("limit") int limit);
}
