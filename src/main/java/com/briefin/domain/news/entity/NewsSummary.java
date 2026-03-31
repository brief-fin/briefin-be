package com.briefin.domain.news.entity;

import com.briefin.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "news_summaries")
public class NewsSummary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id", nullable = false, unique = true)
    private News news;

    @Column(columnDefinition = "TEXT")
    private String summaryLine;

    @Column(length = 20)
    private String category;

    @Column(length = 20)
    private String region;

    @Column(columnDefinition = "TEXT")
    private String titleKo;
}
