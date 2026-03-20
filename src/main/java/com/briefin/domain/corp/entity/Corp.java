package com.briefin.domain.corp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Corp.java
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Corp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String corpCode;   // DART 고유번호 (ex. 00126380)
    private String corpName;   // 기업명
    private String stockCode;  // 종목코드 (ex. 005930), 비상장이면 null
}
