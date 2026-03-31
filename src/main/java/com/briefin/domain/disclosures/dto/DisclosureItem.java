package com.briefin.domain.disclosures.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DisclosureItem {
    private String corp_code;
    private String corp_name;
    private String rcept_no;
    private String report_nm;
    private String rcept_dt;

    @JsonProperty("pblntf_ty")  // 추가 - 명시적 매핑
    private String pblntf_ty;
}