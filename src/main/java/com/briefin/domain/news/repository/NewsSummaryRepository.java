package com.briefin.domain.news.repository;

import com.briefin.domain.news.entity.NewsSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsSummaryRepository extends JpaRepository<NewsSummary, Long> {

    Optional<NewsSummary> findByNewsId(Long newsId);

    @Query("SELECT ns FROM NewsSummary ns JOIN FETCH ns.news ORDER BY ns.news.publishedAt DESC")
    List<NewsSummary> findAllWithNews();

    @Query("SELECT ns FROM NewsSummary ns JOIN FETCH ns.news WHERE ns.category = :category ORDER BY ns.news.publishedAt DESC")
    List<NewsSummary> findByCategoryWithNews(String category);

    @Query(value = "SELECT ns FROM NewsSummary ns JOIN FETCH ns.news ORDER BY ns.news.publishedAt DESC",
           countQuery = "SELECT COUNT(ns) FROM NewsSummary ns")
    Page<NewsSummary> findAllWithNewsPage(Pageable pageable);

    @Query(value = "SELECT ns FROM NewsSummary ns JOIN FETCH ns.news WHERE ns.category = :category ORDER BY ns.news.publishedAt DESC",
           countQuery = "SELECT COUNT(ns) FROM NewsSummary ns WHERE ns.category = :category")
    Page<NewsSummary> findByCategoryWithNewsPage(@Param("category") String category, Pageable pageable);

    List<NewsSummary> findByNewsIdIn(List<Long> newsIds);

    @Query("SELECT ns FROM NewsSummary ns JOIN FETCH ns.news WHERE ns.region = :region ORDER BY ns.news.publishedAt DESC")
    List<NewsSummary> findTop3ByRegionWithNews(@Param("region") String region, Pageable pageable);
}
