package com.briefin.domain.news.converter;

import com.briefin.domain.news.dto.*;
import com.briefin.domain.news.entity.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class NewsConverter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static String resolveTitle(News news, NewsSummary summary) {
        if (summary != null && "해외".equals(summary.getRegion()) && summary.getTitleKo() != null) {
            return summary.getTitleKo();
        }
        return news.getTitle();
    }

    public static NewsListResponseDTO toListDTO(News news, NewsSummary summary, List<NewsCompany> companies) {
        return new NewsListResponseDTO(
                news.getId().toString(),
                resolveTitle(news, summary),
                summary != null ? summary.getSummaryLine() : null,
                summary != null ? summary.getCategory() : null,
                summary != null ? summary.getRegion() : null,
                news.getSource(),
                news.getPublishedAt() != null ? news.getPublishedAt().format(DATE_FORMATTER) : null,
                toCompanyNames(companies)
        );
    }

    public static NewsDetailResponseDTO toDetailDTO(News news, NewsSummary summary,
                                                    List<NewsCompany> companies, List<String> relatedNewsIds,
                                                    boolean isScraped) {
        return new NewsDetailResponseDTO(
                news.getId().toString(),
                resolveTitle(news, summary),
                news.getContent(),
                summary != null ? summary.getSummaryLine() : null,
                summary != null ? summary.getCategory() : null,
                news.getSource(),
                news.getPublishedAt() != null ? news.getPublishedAt().format(DATE_FORMATTER) : null,
                news.getOriginalUrl(),
                news.getThumbnailUrl(),
                isScraped,
                toCompanyInfos(companies),
                relatedNewsIds
        );
    }

    public static NewsSearchResponseDTO toSearchDTO(News news, NewsSummary summary, List<NewsCompany> companies) {
        return new NewsSearchResponseDTO(
                news.getId().toString(),
                resolveTitle(news, summary),
                summary != null ? summary.getSummaryLine() : null,
                summary != null ? summary.getCategory() : null,
                news.getSource(),
                news.getPublishedAt() != null ? news.getPublishedAt().format(DATE_FORMATTER) : null,
                toCompanyNames(companies)
        );
    }

    public static NewsRelatedResponseDTO toRelatedDTO(News news, NewsSummary summary) {
        return new NewsRelatedResponseDTO(
                news.getId().toString(),
                resolveTitle(news, summary),
                summary != null ? summary.getSummaryLine() : null,
                summary != null ? summary.getCategory() : null,
                news.getSource(),
                news.getPublishedAt() != null ? news.getPublishedAt().format(DATE_FORMATTER) : null
        );
    }

    public static NewsTimelineItemDTO toTimelineItemDTO(News news, NewsSummary summary, boolean isCurrent) {
        return new NewsTimelineItemDTO(
                news.getId().toString(),
                resolveTitle(news, summary),
                summary != null ? summary.getSummaryLine() : null,
                summary != null ? summary.getCategory() : null,
                news.getSource(),
                news.getThumbnailUrl(),
                news.getPublishedAt() != null ? news.getPublishedAt().format(DATE_FORMATTER) : null,
                isCurrent
        );
    }

    private static List<String> toCompanyNames(List<NewsCompany> companies) {
        return companies.stream()
                .map(nc -> nc.getCompany().getName())
                .toList();
    }

    private static List<NewsDetailResponseDTO.CompanyInfo> toCompanyInfos(List<NewsCompany> companies) {
        return companies.stream()
                .map(nc -> new NewsDetailResponseDTO.CompanyInfo(
                        nc.getCompany().getId().toString(),
                        nc.getCompany().getName(),
                        nc.getCompany().getTicker()
                ))
                .toList();
    }
}
