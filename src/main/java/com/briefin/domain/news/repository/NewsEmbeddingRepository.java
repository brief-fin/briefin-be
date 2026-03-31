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
            SELECT DISTINCT n.id
            FROM news n
            JOIN news_embeddings ne ON ne.news_id = n.id
            JOIN news_embeddings target ON target.news_id = :newsId
            JOIN news_companies nc ON nc.news_id = n.id
            JOIN news_companies target_nc ON target_nc.news_id = :newsId
            LEFT JOIN news_summaries ns ON ns.news_id = n.id
            LEFT JOIN news_summaries target_ns ON target_ns.news_id = :newsId
            WHERE n.id != :newsId
              AND nc.company_id = target_nc.company_id
              AND (
                1 - (ne.embedding <=> target.embedding) >= 0.4
                OR (
                  1 - (ne.embedding <=> target.embedding) >= 0.2
                  AND ns.category = target_ns.category
                )
              )
            ORDER BY n.published_at ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findTimelineNewsIds(@Param("newsId") Long newsId, @Param("limit") int limit);
}
