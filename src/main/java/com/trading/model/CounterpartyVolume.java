package com.trading.model;

import java.math.BigDecimal;

public record CounterpartyVolume(
        String name,
        BigDecimal volume
) {}