package com.briefin.domain.disclosures.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

public class DartListResponseDTO {

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DartListResponse {  // static 추가
        private String status;
        private String message;
        private List<DisclosureItem> list;
        private int totalCount;
        private int totalPage;
        private int pageNo;
        private int pageCount;
    }
}