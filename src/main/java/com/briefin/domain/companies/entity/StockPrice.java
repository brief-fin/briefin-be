package com.briefin.domain.companies.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StockPrice {
    private double currentPrice;
    private double changeRate;
}
