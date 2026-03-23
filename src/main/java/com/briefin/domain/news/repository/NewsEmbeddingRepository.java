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
}
