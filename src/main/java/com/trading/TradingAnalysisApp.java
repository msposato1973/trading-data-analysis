package com.trading;

import com.trading.service.TradeProcessor;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TradingAnalysisApp {

    public static void main(String[] args) {
        Path dataDir = Paths.get(args.length > 0 ? args[0] : "sample_data");
        System.out.println("Processing trading data in directory: " + dataDir.toAbsolutePath());

        long startTime = System.currentTimeMillis();

        TradeProcessor processor = new TradeProcessor();
        processor.processAndReport(dataDir);

        System.out.printf("Total execution time: %d ms%n", (System.currentTimeMillis() - startTime));
    }
}