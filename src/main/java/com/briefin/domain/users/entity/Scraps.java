package com.briefin.domain.users.entity;

import com.briefin.domain.news.entity.News;
import com.briefin.domain.users.entity.Users;
import com.briefin.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "scraps",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "news_id"})
)
public class Scraps extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id", nullable = false)
    private News news;
}
