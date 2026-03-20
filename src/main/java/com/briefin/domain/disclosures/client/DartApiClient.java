package com.briefin.domain.disclosures.client;

import com.briefin.domain.corp.entity.Corp;
import com.briefin.domain.corp.repository.CorpRepository;
import com.briefin.domain.disclosures.dto.DartListResponseDTO;
import com.briefin.domain.disclosures.dto.DisclosureItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class DartApiClient {

    @Value("${dart.api.key}")
    private String dartApiKey;

    private final RestTemplate restTemplate;
    private final CorpRepository corpRepository;

    // ① corp_code 전체 동기화 (최초 1회)
    public void syncCorpCodes() throws Exception {
        String url = "https://opendart.fss.or.kr/api/corpCode.xml?crtfc_key=" + dartApiKey;
        byte[] zipBytes = restTemplate.getForObject(url, byte[].class);

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".xml")) {
                    parseAndSaveCorpCodes(zis.readAllBytes());
                }
            }
        }
    }

    private void parseAndSaveCorpCodes(byte[] xmlBytes) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        org.w3c.dom.Document doc = builder.parse(new ByteArrayInputStream(xmlBytes));
        NodeList list = doc.getElementsByTagName("list");

        for (int i = 0; i < list.getLength(); i++) {
            Element el = (Element) list.item(i);
            String corpCode  = el.getElementsByTagName("corp_code").item(0).getTextContent().trim();
            String corpName  = el.getElementsByTagName("corp_name").item(0).getTextContent().trim();
            String stockCode = el.getElementsByTagName("stock_code").item(0).getTextContent().trim();

            if (stockCode.isEmpty()) continue;
            if (corpRepository.existsByCorpCode(corpCode)) continue;

            corpRepository.save(Corp.builder()
                    .corpCode(corpCode)
                    .corpName(corpName)
                    .stockCode(stockCode)
                    .build());
        }
    }

    // ② 전체 공시 목록 조회
    public List<DisclosureItem> fetchAllDisclosures(String startDate, String endDate) {
        return fetchList(null, startDate, endDate);
    }

    // ③ 기업별 공시 목록 조회
    public List<DisclosureItem> fetchDisclosuresByCorpCode(String corpCode, String startDate, String endDate) {
        return fetchList(corpCode, startDate, endDate);
    }

    // ④ 공시 원문 텍스트 추출
    public String fetchDisclosureText(String rceptNo) {
        try {
            String viewerUrl = "https://dart.fss.or.kr/dsaf001/main.do?rcpNo=" + rceptNo;
            Document doc = Jsoup.connect(viewerUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(10_000)
                    .get();

            String iframeSrc = doc.select("iframe#ifrm").attr("src");
            if (iframeSrc.isEmpty()) return doc.body().text();

            Document bodyDoc = Jsoup.connect("https://dart.fss.or.kr" + iframeSrc)
                    .userAgent("Mozilla/5.0")
                    .timeout(10_000)
                    .get();

            return bodyDoc.body().text();

        } catch (IOException e) {
            log.error("원문 파싱 실패: {}", rceptNo);
            return "";
        }
    }

    // ⑤ 공통 목록 조회
    private List<DisclosureItem> fetchList(String corpCode, String startDate, String endDate) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl("https://opendart.fss.or.kr/api/list.json")
                .queryParam("crtfc_key", dartApiKey)
                .queryParam("bgn_de", startDate)
                .queryParam("end_de", endDate)
                .queryParam("page_no", 1)
                .queryParam("page_count", 100);

        if (corpCode != null) {
            builder.queryParam("corp_code", corpCode);
        }

        DartListResponseDTO.DartListResponse response = restTemplate.getForObject(
                builder.toUriString(), DartListResponseDTO.DartListResponse.class
        );

        if (response == null || !"000".equals(response.getStatus())) {
            log.error("DART API 오류: {}", response != null ? response.getMessage() : "응답 없음");
            return List.of();
        }

        return response.getList() != null ? response.getList() : List.of();
    }
}