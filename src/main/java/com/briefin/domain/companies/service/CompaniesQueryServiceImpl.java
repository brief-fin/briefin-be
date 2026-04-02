package com.briefin.domain.companies.service;

import com.briefin.domain.companies.client.KisClient;
import com.briefin.domain.companies.dto.CompanyResponseDto;
import com.briefin.domain.companies.dto.CompanyTimelineItemDTO;
import com.briefin.domain.companies.entity.Companies;
import com.briefin.domain.companies.repository.CompaniesRepository;
import com.briefin.domain.disclosures.entity.Disclosures;
import com.briefin.domain.disclosures.repository.DisclosuresRepository;
import com.briefin.domain.news.entity.News;
import com.briefin.domain.news.repository.NewsCompanyRepository;
import com.briefin.domain.news.repository.NewsSummaryRepository;
import com.briefin.domain.users.entity.Users;
import com.briefin.domain.users.repository.UsersRepository;
import com.briefin.domain.users.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompaniesQueryServiceImpl implements CompaniesQueryService {


    private final CompaniesRepository companiesRepository;
    private final KisClient kisClient;
    private final UsersRepository usersRepository;
    private final WatchlistRepository watchlistRepository;
    private final DisclosuresRepository disclosuresRepository;
    private final NewsCompanyRepository newsCompanyRepository;
    private final NewsSummaryRepository newsSummaryRepository;

    @Override
    public CompanyResponseDto getCompany(Long id, UUID userId) {
        Companies company = companiesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("기업을 찾을 수 없습니다. id: " + id));

        boolean watchlisted = false;
        if (userId != null) {
            Users user = usersRepository.findById(userId).orElse(null);
            if (user != null) {
                watchlisted = watchlistRepository.existsByUser_IdAndCompany_Id(user.getId(), company.getId());
            }
        }

        return CompanyResponseDto.builder()
                .id(company.getId())
                .name(company.getName())
                .ticker(company.getTicker())
                .sector(company.getSector())
                .logoUrl(company.getLogoUrl())
                .currentPrice(company.getCurrentPrice() != null ? company.getCurrentPrice().doubleValue() : null)
                .changeRate(company.getChangeRate() != null ? company.getChangeRate().doubleValue() : null)
                .marketCap(company.getMarketCap())
                .isOverseas(company.isOverseas())
                .watchlisted(watchlisted)
                .relatedCompanies(null)
                .build();
    }



    @Override
    public List<CompanyResponseDto> getPopularCompanies() {
        List<String> popularTickers = kisClient.getPopularTickers();
        log.info("HTS 인기 종목코드 {}개 조회: {}", popularTickers.size(), popularTickers);

        List<Companies> companies = companiesRepository.findByTickerIn(popularTickers);
        log.info("DB 매칭 기업 {}개: {}", companies.size(),
                companies.stream().map(Companies::getName).collect(java.util.stream.Collectors.toList()));

        return companies.stream()
                .map(company -> CompanyResponseDto.builder()
                        .id(company.getId())
                        .name(company.getName())
                        .ticker(company.getTicker())
                        .sector(company.getSector())
                        .logoUrl(company.getLogoUrl())
                        .currentPrice(company.getCurrentPrice() != null ? company.getCurrentPrice().doubleValue() : null)
                        .changeRate(company.getChangeRate() != null ? company.getChangeRate().doubleValue() : null)
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Page<CompanyResponseDto> getSearchResultCompanies(String name,Pageable pageable) {

        Page<Companies> companies = companiesRepository.searchByNameOrTicker(name, pageable);



        return companies.map(company -> CompanyResponseDto.builder()
                .id(company.getId())
                .name(company.getName())
                .ticker(company.getTicker())
                .sector(company.getSector())
                .logoUrl("https://thumb.tossinvest.com/image/resized/96x0/https%3A%2F%2Fstatic.toss.im%2Fpng-icons%2Fsecurities%2Ficn-sec-fill-" + company.getTicker() + ".png")
                .currentPrice(company.getCurrentPrice() != null ? company.getCurrentPrice().doubleValue() : null)
                .changeRate(company.getChangeRate() != null ? company.getChangeRate().doubleValue() : null)
                .isOverseas(company.isOverseas())
                .build());
    }

    @Override
    public List<CompanyTimelineItemDTO> getCompanyTimeline(Long companyId) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<CompanyTimelineItemDTO> result = new ArrayList<>();

        // 공시
        disclosuresRepository.findTop30ByCompanyIdOrderByDisclosedAtDesc(companyId).stream()
                .map(d -> new CompanyTimelineItemDTO(
                        "공시",
                        String.valueOf(d.getId()),
                        d.getTitle(),
                        d.getSummary(),
                        d.getCategory(),
                        d.getDisclosedAt() != null ? d.getDisclosedAt().format(dateFormatter) : null,
                        null
                ))
                .forEach(result::add);

        // 기업이벤트 뉴스
        List<News> eventNews = newsCompanyRepository.findEventNewsByCompanyId(companyId);
        List<Long> newsIds = eventNews.stream().map(News::getId).collect(Collectors.toList());
        var summaryMap = newsIds.isEmpty() ? java.util.Map.<Long, com.briefin.domain.news.entity.NewsSummary>of() :
                newsSummaryRepository.findByNewsIdIn(newsIds).stream()
                        .collect(Collectors.toMap(ns -> ns.getNews().getId(), ns -> ns));

        eventNews.stream()
                .map(n -> {
                    var summary = summaryMap.get(n.getId());
                    return new CompanyTimelineItemDTO(
                            "뉴스",
                            String.valueOf(n.getId()),
                            n.getTitle(),
                            summary != null ? summary.getSummaryLine() : null,
                            summary != null ? summary.getCategory() : null,
                            n.getPublishedAt() != null ? n.getPublishedAt().format(dateTimeFormatter) : null,
                            null
                    );
                })
                .forEach(result::add);

        result.sort(Comparator.comparing(
                item -> item.date() != null ? item.date() : "",
                Comparator.naturalOrder()
        ));

        return result;
    }
}
