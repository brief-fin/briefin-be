package com.briefin.domain.news.repository;

import com.briefin.domain.news.entity.NewsSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsSummaryRepository extends JpaRepository<NewsSummary, Long> {

    Optional<NewsSummary> findByNewsId(Long newsId);

    @Query("SELECT ns FROM NewsSummary ns JOIN FETCH ns.news")
    List<NewsSummary> findAllWithNews();

    @Query("SELECT ns FROM NewsSummary ns JOIN FETCH ns.news WHERE ns.category = :category")
    List<NewsSummary> findByCategoryWithNews(String category);

    List<NewsSummary> findByNewsIdIn(List<Long> newsIds);
}
