package com.briefin.domain.news.repository;

import com.briefin.domain.news.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    @Query("SELECT n FROM News n WHERE n.title LIKE %:q% OR n.content LIKE %:q%")
    List<News> searchByKeyword(@Param("q") String q);

    @Query("SELECT n FROM News n WHERE n.id != :newsId ORDER BY n.publishedAt DESC")
    List<News> findRelatedNews(@Param("newsId") Long newsId, Pageable pageable);
}
