package com.briefin.domain.news.repository;

import com.briefin.domain.news.entity.NewsSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsSummaryRepository extends JpaRepository<NewsSummary, Long> {

    Optional<NewsSummary> findByNewsId(Long newsId);

    List<NewsSummary> findByCategory(String category);
}
