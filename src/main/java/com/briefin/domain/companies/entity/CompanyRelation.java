package com.briefin.domain.companies.entity;

import com.briefin.global.common.BaseEntity;
import jakarta.persistence.*;

import lombok.*;

@Entity
@Table(name = "company_relations")
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CompanyRelation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Companies company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_company_id")
    private Companies relatedCompany;
}