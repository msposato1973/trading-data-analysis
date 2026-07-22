package com.trading.model;

import java.math.BigDecimal;

public record Trade(
        String counterParty,
        String ticker,
        String side,
        long shares,
        BigDecimal price
) {
    /**
     * Fast parsing via manual substring operations for maximum speed and minimal impact on the Garbage Collector.
     */
    public static Trade parseFast(String line) {
        int idxCP = line.indexOf('\t');
        if (idxCP == -1) return null;
        int idxTicker = line.indexOf('\t', idxCP + 1);
        if (idxTicker == -1) return null;
        int idxSide = line.indexOf('\t', idxTicker + 1);
        if (idxSide == -1) return null;
        int idxS = line.indexOf('\t', idxSide + 1);
        if (idxS == -1) return null;
        int idxShares = line.indexOf('\t', idxS + 1);
        if (idxShares == -1) return null;

        try {
            String counterParty = line.substring(idxCP + 1, idxTicker).trim();
            String ticker = line.substring(idxTicker + 1, idxSide).trim();
            String side = line.substring(idxSide + 1, idxS).trim();
            long shares = Long.parseLong(line.substring(idxS + 1, idxShares).trim());
            BigDecimal price = new BigDecimal(line.substring(idxShares + 1).trim());

            return new Trade(counterParty, ticker, side, shares, price);
        } catch (Exception e) {
            // Ignore any header or malformed lines
            return null;
        }
    }

    public long getNetSharesChangeForUs() {
        return "SELL".equalsIgnoreCase(side) ? shares : -shares;
    }

    public BigDecimal calculateVolume() {
        return price.multiply(BigDecimal.valueOf(shares));
    }
}