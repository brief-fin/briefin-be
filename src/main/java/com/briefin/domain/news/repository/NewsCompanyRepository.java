package com.briefin.domain.news.repository;

import com.briefin.domain.news.entity.NewsCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsCompanyRepository extends JpaRepository<NewsCompany, Long> {

    List<NewsCompany> findByNewsId(Long newsId);
}
