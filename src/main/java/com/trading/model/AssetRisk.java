package com.trading.model;

import java.math.BigDecimal;
import java.util.Map;

public record AssetRisk(
        String ticker,
        long netShares,
        BigDecimal markPrice,
        BigDecimal marketValue
) {
    public static AssetRisk of(String ticker, long netShares, Map<String, BigDecimal> markPrices) {
        BigDecimal mark = markPrices.getOrDefault(ticker, BigDecimal.ZERO);
        if (mark == null) {
            mark = BigDecimal.ZERO;
        }
        BigDecimal marketValue = mark.multiply(BigDecimal.valueOf(netShares));
        return new AssetRisk(ticker, netShares, mark, marketValue);
    }
}