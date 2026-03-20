package com.briefin.domain.disclosures.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

public class DartListResponseDTO {

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DartListResponse {  // static 추가
        private String status;
        private String message;
        private List<DisclosureItem> list;

        @JsonProperty("total_count")
        private int totalCount;

        @JsonProperty("total_page")
        private int totalPage;

        private int pageNo;
        private int pageCount;
    }
}