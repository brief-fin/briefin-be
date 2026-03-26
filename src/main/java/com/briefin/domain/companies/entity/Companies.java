package com.briefin.domain.companies.entity;

import com.briefin.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "companies")
public class Companies extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String ticker;

    private String sector;

    private String logoUrl;

    private BigDecimal currentPrice;

    private BigDecimal changeRate;

    private Integer marketCap;

    private boolean isOverseas;

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    private List<CompanyRelation> relatedCompanies;

    @Column(nullable = false, unique = true)
    private String corpCode;


    public void update(String corpName, String stockCode) {
        this.name = corpName;
        this.ticker = stockCode;
    }
}