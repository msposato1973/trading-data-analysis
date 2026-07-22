package com.trading.service;

import com.trading.model.AssetRisk;
import com.trading.model.CounterpartyVolume;
import com.trading.model.Trade;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class TradeProcessor {

    private static final Pattern TRADE_FILE_PATTERN = Pattern.compile("^x[a-z]{2}$");

    public void processAndReport(Path dataDir) {
        if (!Files.exists(dataDir) || !Files.isDirectory(dataDir)) {
            System.err.println("Error: Directory non valida -> " + dataDir.toAbsolutePath());
            return;
        }

        // 1. Loading quotes
        Map<String, BigDecimal> markPrices = loadMarks(dataDir.resolve("marks.txt"));
        System.out.println("Loaded " + markPrices.size() + " asset mark prices.\n");

        // 2. Scanning of trade files (x**)
        List<Path> tradeFiles;
        try (Stream<Path> pathStream = Files.list(dataDir)) {
            tradeFiles = pathStream
                    .filter(Files::isRegularFile)
                    .filter(p -> TRADE_FILE_PATTERN.matcher(p.getFileName().toString()).matches())
                    .toList();

            System.out.println("Found " + tradeFiles.size() + " trade file(s) to process.");
        } catch (IOException e) {
            System.err.println("Error listing data files: " + e.getMessage());
            return;
        }

        // Maps for the final merging of results
        Map<String, long[]> aggregatedNetPositions = new ConcurrentHashMap<>();
        Map<String, BigDecimal> aggregatedCounterpartyVolumes = new ConcurrentHashMap<>();

        // 3. Parallel Execution via Virtual Threads (Java 21)
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Path filePath : tradeFiles) {
                executor.submit(() -> {
                    // Mappe locali per azzerare la lock contention tra thread
                    Map<String, long[]> localNetPositions = new HashMap<>();
                    Map<String, BigDecimal> localVolumes = new HashMap<>();

                    processSingleFile(filePath, localNetPositions, localVolumes);

                    // Merge local results into global maps
                    localNetPositions.forEach((ticker, shares) ->
                            aggregatedNetPositions.computeIfAbsent(ticker, k -> new long[1])[0] += shares[0]
                    );

                    localVolumes.forEach((cp, vol) ->
                            aggregatedCounterpartyVolumes.merge(cp, vol, BigDecimal::add)
                    );
                });
            }
        }
        // The try-with-resources waits for the completion of ALL virtual threads.
        // 4. Report's print
        printTopTwentyAssetRisk(aggregatedNetPositions, markPrices);
        printTopTwentyCounterparties(aggregatedCounterpartyVolumes);
    }

    private void processSingleFile(Path filePath, Map<String, long[]> localPositions, Map<String, BigDecimal> localVolumes) {
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;

                Trade trade = Trade.parseFast(line);
                if (trade == null) continue;

                // Local aggregation
                localPositions.computeIfAbsent(trade.ticker(), k -> new long[1])[0] += trade.getNetSharesChangeForUs();
                localVolumes.merge(trade.counterParty(), trade.calculateVolume(), BigDecimal::add);
            }
        } catch (IOException e) {
            System.err.println("Error reading file " + filePath.getFileName() + ": " + e.getMessage());
        }
    }

    private Map<String, BigDecimal> loadMarks(Path marksFile) {
        Map<String, BigDecimal> marksMap = new HashMap<>();
        if (!Files.exists(marksFile)) return marksMap;

        try (BufferedReader reader = Files.newBufferedReader(marksFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) {
                    try {
                        marksMap.put(parts[0].trim(), new BigDecimal(parts[1].trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (IOException ignored) {}
        return marksMap;
    }

    private void printTopTwentyAssetRisk(Map<String, long[]> tickerNetPositions, Map<String, BigDecimal> markPrices) {
        List<AssetRisk> topLongAssets = tickerNetPositions.entrySet().stream()
                .filter(entry -> entry.getValue()[0] > 0)
                .map(entry -> AssetRisk.of(entry.getKey(), entry.getValue()[0], markPrices))
                .sorted(Comparator.comparing(AssetRisk::marketValue).reversed())
                .limit(20)
                .toList();

        System.out.println("=========================================================================================");
        System.out.println("                    REPORT 1: TOP 20 LONG ASSET RISK (MARKET VALUE)                      ");
        System.out.println("=========================================================================================");
        System.out.printf("%-5s | %-10s | %-15s | %-15s | %-20s%n", "Rank", "Ticker", "Net Long Shares", "Mark Price ($)", "Market Value ($)");
        System.out.println("-----------------------------------------------------------------------------------------");

        int rank = 1;
        for (AssetRisk asset : topLongAssets) {
            System.out.printf("%-5d | %-10s | %,15d | %,15.2f | %,20.2f%n",
                    rank++, asset.ticker(), asset.netShares(), asset.markPrice(), asset.marketValue().setScale(2, RoundingMode.HALF_UP));
        }
        System.out.println("=========================================================================================\n");
    }

    private void printTopTwentyCounterparties(Map<String, BigDecimal> counterpartyVolumes) {
        List<CounterpartyVolume> topCounterparties = counterpartyVolumes.entrySet().stream()
                .map(entry -> new CounterpartyVolume(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CounterpartyVolume::volume).reversed())
                .limit(20)
                .toList();

        System.out.println("=========================================================================================");
        System.out.println("               REPORT 2: TOP 20 COUNTERPARTIES BY TRADING DOLLAR VOLUME                 ");
        System.out.println("=========================================================================================");
        System.out.printf("%-5s | %-45s | %-25s%n", "Rank", "Counterparty Name", "Total Dollar Volume ($)");
        System.out.println("-----------------------------------------------------------------------------------------");

        int rank = 1;
        for (CounterpartyVolume cp : topCounterparties) {
            System.out.printf("%-5d | %-45s | %,25.2f%n",
                    rank++, cp.name(), cp.volume().setScale(2, RoundingMode.HALF_UP));
        }
        System.out.println("=========================================================================================\n");
    }
}