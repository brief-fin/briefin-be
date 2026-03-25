package com.briefin.domain.companies.event;

import com.briefin.domain.companies.entity.StockPrice;


public record StockPriceUpdatedEvent(String ticker, double price, double diff) {
}

