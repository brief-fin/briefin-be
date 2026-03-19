package com.briefin.domain.news.entity;

import com.briefin.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "news_embeddings")
public class NewsEmbedding extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id", nullable = false)
    private News news;

    // pgvector extension 필요: CREATE EXTENSION IF NOT EXISTS vector;
    // vector(768) 타입은 float[] 로 매핑 후 pgvector 드라이버가 변환
    @Column(columnDefinition = "vector(768)")
    private float[] embedding;
}
