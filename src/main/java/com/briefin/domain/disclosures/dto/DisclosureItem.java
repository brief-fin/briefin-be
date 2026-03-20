package com.briefin.domain.disclosures.dto;

import lombok.Data;

@Data
public class DisclosureItem {
    private String corp_code;   // DART 기업 고유번호
    private String corp_name;   // 기업명
    private String rcept_no;    // 접수번호 (dartId로 저장)
    private String report_nm;   // 공시명 (title로 저장)
    private String rcept_dt;    // 공시일자 yyyyMMdd (disclosedAt으로 저장)
}