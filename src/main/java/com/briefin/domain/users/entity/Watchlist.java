package com.briefin.domain.users.entity;

import com.briefin.domain.companies.entity.Companies;
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
        name = "watchlist",
        uniqueConstraints = @UniqueConstraint(columnNames  = {"user_id", "company_id"})
)

public class Watchlist extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private  Users user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Companies company;


}
