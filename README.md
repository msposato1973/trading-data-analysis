# Trading Data Analysis System

A high-performance Java 21 application built to analyze financial trade execution data, compute net long asset risk based on market mark prices, and aggregate trading volume by counterparty.

---

## 1. Requirements

### Context & Business Logic
In this trading data analysis exercise, our net position in each ticker is defined as the net position across all trades executed for that ticker across all counterparties:
* **Buy/Sell Perspective**: Trade sides in the source files are relative to the counterparty.
    * If a record indicates **`BUY`**: The counterparty bought from us $\rightarrow$ **We sold** (our position decreases / net short).
    * If a record indicates **`SELL`**: The counterparty sold to us $\rightarrow$ **We bought** (our position increases / net long).
* **Asset Risk Calculation**:
  $$\text{Net Long Position Value} = (\text{Total Shares Bought} - \text{Total Shares Sold}) \times \text{Current Mark Price}$$
  *(Only positive net share positions, i.e., Net Long $> 0$, are evaluated for Asset Risk)*.
* **Counterparty Trading Volume**:
  $$\text{Trade Value} = \text{Shares} \times \text{Trade Price}$$
  Aggregated across all trades executed with each counterparty.

### Input Data Format
1. **`marks.txt`**: Contains current market prices for risk valuation. Format: Ticker and price separated by whitespace or tabs.
2. **Trade Data Files (`x**`)**: Five tab-separated files without file extensions (e.g., `xaa`, `xab`, `xac`, `xad`, `xae`), each containing 10,000 records structured as follows:
    1. Date (`YYYY-MM-DD`)
    2. Counterparty Name
    3. Ticker Symbol
    4. Buy/Sell Indicator (relative to counterparty)
    5. Number of Shares
    6. Trade Price

#### Sample Rows
```text
2011-01-03	American Funds EuPc;A	AAPL	BUY	2400	332.87
2011-02-14	American Funds CWGI;A	SLB	BUY	6700	94.08
2011-01-06	Tudor Investment Corp	ALL	BUY	11800	31.92
2011-01-28	American Funds CWGI;A	NYX	BUY	10000	31.49
```
#### Deliverables & Output Reports
The application must process all trade data files in a directory and generate two console reports:

Report 1: Asset Risk — Top 20 assets where we are net long, including net share counts, mark prices, and total market value.

Report 2: Counterparty Volume — Top 20 counterparties ranked by total dollar trading volume (market value at time of trade).

#### 2. Solution Architecture & Technical Implementation

### Key Technical Highlights
- Java 21 Virtual Threads: File processing runs concurrently via Executors.newVirtualThreadPerTaskExecutor(). This leverages lightweight Virtual Threads to handle I/O operations without pinning platform OS threads.

- Lock-Free Map-Reduce Pattern: To eliminate lock contention across threads, each Virtual Thread aggregates trade records into thread-local standard HashMaps. A thread-safe reduction merges local maps into global ConcurrentHashMap instances only once per file.

- Fast In-Memory Parsing: Avoids heavy regex allocations and String.split() calls by using direct tab character index scans (indexOf('\t')) and custom primitives (long[] array containers instead of primitive wrapper auto-boxing).

- Domain Records: Immutable Java 21 record constructs represent data models (Trade, AssetRisk, CounterpartyVolume), enforcing strict immutability and encapsulated domain logic.

### Project Structure
```text
trading-data-analysis/
├── pom.xml
├── README.md
├── sample_data/
│   ├── marks.txt
│   ├── xaa
│   ├── xab
│   └── ...
└── src/
└── main/
└── java/
└── com/
└── trading/
├── TradingAnalysisApp.java          # Main Application Entry Point
├── model/
│   ├── AssetRisk.java               # Asset Risk Record
│   ├── CounterpartyVolume.java      # Counterparty Volume Record
│   └── Trade.java                   # Trade Record with Fast Parsing
└── service/
└── TradeProcessor.java              # Multi-threaded Core Engine

```
## 3. How to Build and Run
###   Prerequisites
   - Java JDK 21 or higher
   - Apache Maven 3.8+

## Step 1: Build the Project
Compile and package the application into an executable JAR:

```maven 
mvn clean package
```

```bash
java -jar target/trading-data-analysis-1.0-SNAPSHOT.jar ./sample_data
```

(If no directory argument is passed, it defaults to looking for ./sample_data in the working directory).

4. Sample Console Output
```text
Processing trading data in directory: C:\Users\massimo.sposato\WS\trading-data-analysis\sample_data
Loaded 500 asset mark prices.

Found 5 trade file(s) to process.

=========================================================================================
REPORT 1: TOP 20 LONG ASSET RISK (MARKET VALUE)                      
=========================================================================================
Rank  | Ticker     | Net Long Shares | Mark Price ($)  | Market Value ($)
-----------------------------------------------------------------------------------------
1     | AAPL       |          45,200 |          332.87 |        15,045,724.00
2     | SLB        |          38,100 |           94.08 |         3,584,448.00
3     | ALL        |          89,400 |           31.92 |         2,853,648.00
4     | NYX        |          61,000 |           31.49 |         1,920,890.00
...

=========================================================================================
REPORT 2: TOP 20 COUNTERPARTIES BY TRADING DOLLAR VOLUME                 
=========================================================================================
Rank  | Counterparty Name                             |   Total Dollar Volume ($)
-----------------------------------------------------------------------------------------
1     | American Funds EuPc;A                         |          142,850,210.50
2     | Tudor Investment Corp                         |          118,410,900.00
3     | American Funds CWGI;A                         |           98,120,430.25
...
=========================================================================================

Total execution time: 42 ms

```


5. Report result
```text
Loaded 100 asset mark prices.

Found 5 trade file(s) to process.
=========================================================================================
                    REPORT 1: TOP 20 LONG ASSET RISK (MARKET VALUE)                      
=========================================================================================
Rank  | Ticker     | Net Long Shares | Mark Price ($)  | Market Value ($)    
-----------------------------------------------------------------------------------------
1     | ORCL       |       3,656,700 |           33.70 |       123,230,790.00
2     | CVX        |       1,126,800 |          108.29 |       122,021,172.00
3     | AAPL       |         339,100 |          351.11 |       119,061,401.00
4     | GOOG       |         131,000 |          899.76 |       117,868,560.00
5     | F          |       6,908,400 |           15.03 |       103,833,252.00
6     | MRK        |       2,993,000 |           33.05 |        98,918,650.00
7     | PFE        |       3,992,900 |           20.35 |        81,255,515.00
8     | JPM        |       1,713,000 |           46.55 |        79,740,150.00
9     | EMC        |       2,941,500 |           26.94 |        79,244,010.00
10    | KO         |       1,179,900 |           66.89 |        78,923,511.00
11    | BHI        |       1,038,600 |           74.06 |        76,918,716.00
12    | BK         |       2,462,400 |           29.93 |        73,699,632.00
13    | DD         |       1,307,900 |           55.19 |        72,183,001.00
14    | WFC        |       1,570,000 |           45.09 |        70,791,300.00
15    | T          |       2,291,400 |           30.87 |        70,735,518.00
16    | AMZN       |         366,800 |          181.58 |        66,603,544.00
17    | FCX        |       1,184,300 |           55.46 |        65,681,278.00
18    | PM         |         973,400 |           65.87 |        64,117,858.00
19    | CAT        |         565,000 |          112.09 |        63,330,850.00
20    | MET        |         944,400 |           65.95 |        62,283,180.00
=========================================================================================

=========================================================================================
               REPORT 2: TOP 20 COUNTERPARTIES BY TRADING DOLLAR VOLUME                 
=========================================================================================
Rank  | Counterparty Name                             | Total Dollar Volume ($)  
-----------------------------------------------------------------------------------------
1     | American Funds CWGI;A                         |          1,421,604,258.00
2     | Fortress Investment Group                     |          1,276,344,758.00
3     | American Funds EuPc;A                         |          1,198,363,893.00
4     | GLG Partners                                  |          1,172,106,399.00
5     | PIMCO:Tot Rtn;Inst                            |          1,164,432,195.00
6     | Landsdowne Partners                           |          1,134,161,558.00
7     | Soros Fund Mgmt.                              |          1,108,143,493.00
8     | BlueCrest Capital Mgmt.                       |          1,088,779,027.00
9     | iShares :MSCI EAFE Idx                        |          1,081,073,628.00
10    | Farallon Capital Mgmt.                        |          1,065,601,939.00
11    | Goldman Sachs Asset Mgmt.                     |          1,018,639,312.00
12    | Paulson & Co.                                 |          1,012,297,282.00
13    | J.P. Morgan                                   |            971,044,132.00
14    | Federated Prime Obl;Inst                      |            938,321,666.00
15    | Dodge & Cox Intl Stock                        |            927,919,651.00
16    | Brevan Howard                                 |            921,430,969.00
17    | Fidelity Cash Reserves                        |            919,464,752.00
18    | Baupost Group                                 |            884,077,874.00
19    | MSCI Em Mkt ETF                               |            876,401,658.00
20    | American Funds Wash;A                         |            866,732,598.00
=========================================================================================

Total execution time: 190 ms
```