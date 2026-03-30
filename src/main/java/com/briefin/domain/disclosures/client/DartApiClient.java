package com.briefin.domain.disclosures.client;

import com.briefin.domain.companies.entity.Companies;
import com.briefin.domain.companies.repository.CompaniesRepository;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class DartApiClient {

    @Value("${dart.api.key}")
    private String dartApiKey;

    @Value("${dart.api.type-delay-millis:200}")
    private long typeDelayMillis;

    private final RestTemplate restTemplate;
    private final CompaniesRepository companiesRepository;

    // corp_code 전체 동기화 (최초 1회)
    public void syncCorpCodes() throws Exception {
        String url = "https://opendart.fss.or.kr/api/corpCode.xml?crtfc_key=" + dartApiKey;
        byte[] zipBytes = restTemplate.getForObject(url, byte[].class);

        if (zipBytes == null) {
            throw new RuntimeException("DART corp_code ZIP 응답이 없습니다.");
        }

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
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document doc = builder.parse(new ByteArrayInputStream(xmlBytes));
        NodeList list = doc.getElementsByTagName("list");
        for (int i = 0; i < list.getLength(); i++) {
            Element el = (Element) list.item(i);
            String corpCode = el.getElementsByTagName("corp_code").item(0).getTextContent().trim();
            String corpName = el.getElementsByTagName("corp_name").item(0).getTextContent().trim();
            String stockCode = el.getElementsByTagName("stock_code").item(0).getTextContent().trim();
            if (stockCode.isEmpty()) continue;

            companiesRepository.findByCorpCode(corpCode).ifPresentOrElse(
                    existing -> {
                        existing.update(corpName, stockCode);
                        companiesRepository.save(existing);
                    },
                    () -> companiesRepository.save(Companies.builder()
                            .name(corpName)
                            .ticker(stockCode)
                            .corpCode(corpCode)
                            .corpName(corpName)
                            .stockCode(stockCode)
                            .build())
            );
        }
    }

    // 주가에 영향을 미치는 공시 세부유형
    // B001: 주요사항보고서 (유상증자, 무상증자, 감자, 합병, 분할, 배당 등)
    // A001/A002/A003: 사업/반기/분기보고서 (실적)
    // C001: 증권신고서(지분증권) - 신주발행
    // D001: 주식등의대량보유상황보고서 - 5% 룰
    private static final String[] STOCK_RELEVANT_DETAIL_TYPES = {"B001", "A001", "A002", "A003", "C001", "D001"};

    // 전체 공시 목록 조회
    public List<DisclosureItem> fetchAllDisclosures(String startDate, String endDate) {
        List<DisclosureItem> all = new ArrayList<>();

        for (int i = 0; i < STOCK_RELEVANT_DETAIL_TYPES.length; i++) {
            String detailType = STOCK_RELEVANT_DETAIL_TYPES[i];
            all.addAll(fetchList(null, startDate, endDate, detailType));
            sleepBetweenTypeCalls(i);
        }

        return deduplicateByRceptNo(all);
    }

    // 기업별 공시 목록 조회
    public List<DisclosureItem> fetchDisclosuresByCorpCode(String corpCode, String startDate, String endDate) {
        List<DisclosureItem> all = new ArrayList<>();

        for (int i = 0; i < STOCK_RELEVANT_DETAIL_TYPES.length; i++) {
            String detailType = STOCK_RELEVANT_DETAIL_TYPES[i];
            all.addAll(fetchList(corpCode, startDate, endDate, detailType));
            sleepBetweenTypeCalls(i);
        }

        return deduplicateByRceptNo(all);
    }

    private void sleepBetweenTypeCalls(int currentIndex) {
        if (currentIndex >= STOCK_RELEVANT_DETAIL_TYPES.length - 1 || typeDelayMillis <= 0) {
            return;
        }

        try {
            Thread.sleep(typeDelayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("DART API 호출 대기 중 인터럽트가 발생했습니다.", e);
        }
    }

    private List<DisclosureItem> deduplicateByRceptNo(List<DisclosureItem> items) {
        Map<String, DisclosureItem> deduplicated = new LinkedHashMap<>();

        for (DisclosureItem item : items) {
            if (item == null || item.getRcept_no() == null || item.getRcept_no().isBlank()) {
                continue;
            }
            deduplicated.putIfAbsent(item.getRcept_no(), item);
        }

        return new ArrayList<>(deduplicated.values());
    }

    // 공시 원문 텍스트 추출 (DART 공식 원문 다운로드 API 사용)
    public String fetchDisclosureText(String rceptNo) {
        try {
            String url = "https://opendart.fss.or.kr/api/document.xml?crtfc_key=" + dartApiKey + "&rcept_no=" + rceptNo;
            byte[] zipBytes = restTemplate.getForObject(url, byte[].class);

            if (zipBytes == null || zipBytes.length == 0) {
                log.warn("원문 ZIP 응답 없음: {}", rceptNo);
                return "";
            }

            StringBuilder text = new StringBuilder();
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName().toLowerCase();
                    log.debug("원문 ZIP 파일: {} ({})", name, rceptNo);
                    if (name.endsWith(".html") || name.endsWith(".htm") || name.endsWith(".xml")) {
                        Document doc = Jsoup.parse(new String(zis.readAllBytes(), "UTF-8"));
                        doc.select("script, style").remove();
                        String body = doc.body().text().strip();
                        if (!body.isBlank()) {
                            text.append(body).append("\n");
                        }
                    }
                }
            }

            String result = text.toString().strip();
            if (result.isBlank()) {
                log.warn("원문 텍스트 추출 결과 없음: {}", rceptNo);
            }
            return result;

        } catch (Exception e) {
            log.error("원문 파싱 실패: {}", rceptNo, e);
            return "";
        }
    }

    // 공통 목록 조회 (페이지네이션)
    private List<DisclosureItem> fetchList(String corpCode, String startDate, String endDate, String pblntfDetailTy) {
        List<DisclosureItem> allItems = new ArrayList<>();
        int pageNo = 1;

        while (true) {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl("https://opendart.fss.or.kr/api/list.json")
                    .queryParam("crtfc_key", dartApiKey)
                    .queryParam("bgn_de", startDate)
                    .queryParam("end_de", endDate)
                    .queryParam("pblntf_detail_ty", pblntfDetailTy)
                    .queryParam("page_no", pageNo)
                    .queryParam("page_count", 100);

            if (corpCode != null) {
                builder.queryParam("corp_code", corpCode);
            }

            DartListResponseDTO.DartListResponse response = restTemplate.getForObject(
                    builder.toUriString(), DartListResponseDTO.DartListResponse.class
            );

            if (response == null) {
                throw new RuntimeException("DART API 응답이 없습니다.");
            }

            if ("013".equals(response.getStatus())) {
                log.info("조회된 공시가 없습니다.");
                break;
            }

            if (!"000".equals(response.getStatus())) {
                throw new RuntimeException("DART API 오류 [" + response.getStatus() + "]: " + response.getMessage());
            }

            List<DisclosureItem> items = response.getList();
            if (items == null || items.isEmpty()) break;

            items.forEach(item -> item.setPblntf_ty(pblntfDetailTy));
            allItems.addAll(items);

            log.info("공시 목록 조회 - page: {}/{}, 누적: {}건",
                    pageNo, response.getTotalPage(), allItems.size());

            if (pageNo >= response.getTotalPage()) break;

            pageNo++;
        }

        return allItems;
    }
}