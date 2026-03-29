package com.briefin.domain.disclosures.entity;

import com.briefin.domain.companies.entity.Companies;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private String summaryDetail;

    @Column(name = "category", length = 50)
    private String category;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public String getCategory() {
        if (category == null) return "기타";  // null 체크 추가
        return switch (category) {
            case "A" -> "정기공시";
            case "B" -> "주요사항보고";
            case "C" -> "발행공시";
            case "D" -> "지분공시";
            case "E" -> "기타공시";
            case "F" -> "외부감사관련";
            case "G" -> "펀드공시";
            case "H" -> "자산유동화";
            case "I" -> "거래소공시";
            case "J" -> "공정위공시";
            default -> "기타";
        };
    }

    public void updateSummaryDetail(String summaryDetail) {
        this.summaryDetail = summaryDetail;
    }
}