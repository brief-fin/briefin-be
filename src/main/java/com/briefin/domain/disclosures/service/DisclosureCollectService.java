package com.briefin.domain.disclosures.service;

public interface DisclosureCollectService {
    void syncCorpCodes() throws Exception;
    void collectAll(String startDate, String endDate);           // 추가
    void collectByCorpCode(String corpCode, String startDate, String endDate);  // 추가
}
