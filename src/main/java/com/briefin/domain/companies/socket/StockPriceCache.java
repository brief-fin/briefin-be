package com.briefin.domain.companies.socket;

import com.briefin.domain.companies.entity.StockPrice;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StockPriceCache {
    private final Map<String, StockPrice> cache = new ConcurrentHashMap<>();

    public void update(String ticker, StockPrice price) {
        cache.put(ticker, price);
    }

    public StockPrice get(String ticker) {
        return cache.get(ticker);
    }

    public Map<String, StockPrice> getAll() {
        return Collections.unmodifiableMap(cache);
    }
}