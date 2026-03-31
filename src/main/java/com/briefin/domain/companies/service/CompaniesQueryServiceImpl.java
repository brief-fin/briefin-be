package com.briefin.domain.companies.service;

import com.briefin.domain.companies.client.KisClient;
import com.briefin.domain.companies.dto.CompanyResponseDto;
import com.briefin.domain.companies.entity.Companies;
import com.briefin.domain.companies.repository.CompaniesRepository;
import com.briefin.domain.users.entity.Users;
import com.briefin.domain.users.repository.UsersRepository;
import com.briefin.domain.users.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompaniesQueryServiceImpl implements CompaniesQueryService {


    private final CompaniesRepository companiesRepository;
    private final KisClient kisClient;
    private final UsersRepository usersRepository;
    private final WatchlistRepository watchlistRepository;

    @Override
    public CompanyResponseDto getCompany(Long id, UUID userId) {
        Companies company = companiesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("기업을 찾을 수 없습니다. id: " + id));

        boolean watchlisted = false;
        if (userId != null) {
            Users user = usersRepository.findById(userId).orElse(null);
            if (user != null) {
                watchlisted = watchlistRepository.existsByUserIdAndCompanyId(user.getId(), company.getId());
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


}
