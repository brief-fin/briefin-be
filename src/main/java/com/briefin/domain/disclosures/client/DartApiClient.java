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
import java.io.IOException;
import java.util.ArrayList;
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
            // skip 대신 upsert로 변경
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

    private static final String[] PBLNTF_TYPES = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};

    // 전체 공시 목록 조회
    public List<DisclosureItem> fetchAllDisclosures(String startDate, String endDate) {
        List<DisclosureItem> all = new ArrayList<>();
        for (String type : PBLNTF_TYPES) {
            all.addAll(fetchList(null, startDate, endDate, type));
        }
        return all;
    }

    // 기업별 공시 목록 조회
    public List<DisclosureItem> fetchDisclosuresByCorpCode(String corpCode, String startDate, String endDate) {
        List<DisclosureItem> all = new ArrayList<>();
        for (String type : PBLNTF_TYPES) {
            all.addAll(fetchList(corpCode, startDate, endDate, type));
        }
        return all;
    }

    // 공시 원문 텍스트 추출
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

            // 네비게이션/UI 요소 제거
            bodyDoc.select("nav, header, footer, script, style, .navigation, #toolbar").remove();

            // table이 있으면 table 텍스트 위주로 추출 (공시 본문은 보통 table)
            String tableText = bodyDoc.select("table").text();
            if (!tableText.isBlank()) {
                return tableText;
            }

            // table 없으면 body 전체
            return bodyDoc.body().text();

        } catch (IOException e) {
            log.error("원문 파싱 실패: {}", rceptNo);
            return "";
        }
    }

    // 공통 목록 조회 (페이지네이션)
    private List<DisclosureItem> fetchList(String corpCode, String startDate, String endDate, String pblntfTy) {
        List<DisclosureItem> allItems = new ArrayList<>();
        int pageNo = 1;

        while (true) {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl("https://opendart.fss.or.kr/api/list.json")
                    .queryParam("crtfc_key", dartApiKey)
                    .queryParam("bgn_de", startDate)
                    .queryParam("end_de", endDate)
                    .queryParam("pblntf_ty", pblntfTy)
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

            // no-data는 정상 (조회 결과 없음)
            if ("013".equals(response.getStatus())) {
                log.info("조회된 공시가 없습니다.");
                break;
            }

            // 그 외 비-000은 실제 오류 (잘못된 API 키, rate limit 등)
            if (!"000".equals(response.getStatus())) {
                throw new RuntimeException("DART API 오류 [" + response.getStatus() + "]: " + response.getMessage());
            }

            List<DisclosureItem> items = response.getList();
            if (items == null || items.isEmpty()) break;

            items.forEach(item -> item.setPblntf_ty(pblntfTy));
            allItems.addAll(items);
            log.info("공시 목록 조회 - page: {}/{}, 누적: {}건",
                    pageNo, response.getTotalPage(), allItems.size());

            if (pageNo >= response.getTotalPage()) break;

            pageNo++;
        }

        return allItems;
    }
}