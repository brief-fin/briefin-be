package com.briefin.domain.disclosures.entity;

import com.briefin.domain.companies.entity.Companies;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "disclosures")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Disclosures {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Companies company;

    @Column(name = "dart_id", length = 100, unique = true, nullable = false)
    private String dartId;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "disclosed_at")
    private LocalDate disclosedAt;

    @Column(name = "url", length = 1000)
    private String url;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String summaryDetail;  // GPT 상세 분석

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateSummaryDetail(String summaryDetail) {
        this.summaryDetail = summaryDetail;
    }
}