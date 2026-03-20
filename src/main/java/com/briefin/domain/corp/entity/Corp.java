package com.briefin.domain.corp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "corp", uniqueConstraints = {
        @UniqueConstraint(columnNames = "corp_code")
})
public class Corp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String corpCode;

    @Column(nullable = false)
    private String corpName;

    private String stockCode;

    public void update(String corpName, String stockCode) {
        this.corpName = corpName;
        this.stockCode = stockCode;
    }
}
