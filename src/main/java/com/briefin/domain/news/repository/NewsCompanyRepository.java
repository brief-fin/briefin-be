package com.briefin.domain.news.repository;

import com.briefin.domain.news.entity.News;
import com.briefin.domain.news.entity.NewsCompany;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsCompanyRepository extends JpaRepository<NewsCompany, Long> {

    @Query("SELECT nc FROM NewsCompany nc JOIN FETCH nc.company WHERE nc.news.id = :newsId")
    List<NewsCompany> findByNewsId(@Param("newsId") Long newsId);

    @Query("SELECT nc FROM NewsCompany nc JOIN FETCH nc.company WHERE nc.news.id IN :newsIds")
    List<NewsCompany> findByNewsIdIn(@Param("newsIds") List<Long> newsIds);

    @Query(value = "SELECT n.* FROM news n WHERE n.id IN (SELECT nc.news_id FROM news_companies nc WHERE nc.company_id = :companyId) ORDER BY n.published_at DESC, n.id DESC",
           countQuery = "SELECT COUNT(*) FROM news_companies WHERE company_id = :companyId",
           nativeQuery = true)
    Page<News> findNewsByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @Query(value = """
            SELECT n.* FROM news n
            JOIN news_companies nc ON nc.news_id = n.id
            JOIN news_summaries ns ON ns.news_id = n.id
            WHERE nc.company_id = :companyId
              AND ns.category = '기업이벤트'
            ORDER BY n.published_at DESC
            LIMIT 30
            """, nativeQuery = true)
    List<News> findEventNewsByCompanyId(@Param("companyId") Long companyId);
}
