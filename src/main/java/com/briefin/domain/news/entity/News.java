package com.briefin.domain.news.entity;

import com.briefin.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "news")
public class News extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 100)
    private String source;

    @Column(nullable = false, unique = true, length = 1000)
    private String originalUrl;

    private LocalDateTime publishedAt;

    @Column(columnDefinition = "TEXT")
    private String thumbnailUrl;
}
