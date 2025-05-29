package com.example.bitcoinmarketrecorder.service;

import com.example.bitcoinmarketrecorder.model.BestBidAsk;
import com.example.bitcoinmarketrecorder.model.MarketBoard;
import com.example.bitcoinmarketrecorder.model.Trade;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DataPersistenceServiceImpl implements DataPersistenceService {

  private static final Logger logger = LoggerFactory.getLogger(DataPersistenceServiceImpl.class);
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
  private final BlockingQueue<Trade> tradeQueue;
  private final BlockingQueue<MarketBoard> boardQueue;
  private final BlockingQueue<BestBidAsk> bestBidAskQueue;
  private final ExecutorService workerExecutor;
  private volatile boolean isRunning = true;

  @Value("${database.csv-dir:csv}")
  private String csvDir;

  public DataPersistenceServiceImpl() {
    this.tradeQueue = new LinkedBlockingQueue<>();
    this.boardQueue = new LinkedBlockingQueue<>();
    this.bestBidAskQueue = new LinkedBlockingQueue<>();
    this.workerExecutor = Executors.newSingleThreadExecutor();
  }

  @PostConstruct
  public void initialize() {
    startWorker();
  }

  private void startWorker() {
    workerExecutor.submit(
        () -> {
          logger.info("Starting data persistence worker thread");
          while (isRunning) {
            try {
              // Process trades
              List<Trade> trades = new ArrayList<>();
              tradeQueue.drainTo(trades, 100); // 最大100件までバッチ処理
              if (!trades.isEmpty()) {
                logger.info("Processing {} trades from queue", trades.size());
                saveTradesToCsv(trades);
              }

              // Process market boards
              List<MarketBoard> boards = new ArrayList<>();
              boardQueue.drainTo(boards, 100);
              if (!boards.isEmpty()) {
                logger.info("Processing {} market boards from queue", boards.size());
                saveMarketBoardsToCsv(boards);
              }

              // Process best bid/ask
              List<BestBidAsk> bestBidAsks = new ArrayList<>();
              bestBidAskQueue.drainTo(bestBidAsks, 100);
              if (!bestBidAsks.isEmpty()) {
                logger.info("Processing {} best bid/ask records from queue", bestBidAsks.size());
                saveBestBidAskToCsv(bestBidAsks);
              }

              // キューが空の場合は少し待機
              if (trades.isEmpty() && boards.isEmpty() && bestBidAsks.isEmpty()) {
                Thread.sleep(100);
              }
            } catch (InterruptedException e) {
              logger.warn("Worker thread interrupted", e);
              Thread.currentThread().interrupt();
              break;
            } catch (Exception e) {
              logger.error("Error in worker thread: {}", e.getMessage(), e);
              // エラーが発生してもワーカースレッドは継続
              try {
                Thread.sleep(1000); // エラー発生時は少し長めに待機
              } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
              }
            }
          }
          logger.info("Data persistence worker thread stopped");
        });
  }

  @Override
  public void saveTrades(List<Trade> trades) {
    try {
      if (trades != null && !trades.isEmpty()) {
        logger.debug("Adding {} trades to queue", trades.size());
        tradeQueue.addAll(trades);
      }
    } catch (Exception e) {
      logger.error("Error adding trades to queue: {}", e.getMessage(), e);
    }
  }

  @Override
  public void saveMarketBoard(MarketBoard board) {
    try {
      if (board != null) {
        logger.debug("Adding market board to queue for symbol: {}", board.getSymbol());
        boardQueue.add(board);
      }
    } catch (Exception e) {
      logger.error("Error adding market board to queue: {}", e.getMessage(), e);
    }
  }

  @Override
  public void saveBestBidAsk(BestBidAsk bestBidAsk) {
    try {
      if (bestBidAsk != null) {
        logger.debug("Adding best bid/ask to queue for symbol: {}", bestBidAsk.getSymbol());
        bestBidAskQueue.add(bestBidAsk);
      }
    } catch (Exception e) {
      logger.error("Error adding best bid/ask to queue: {}", e.getMessage(), e);
    }
  }

  private void saveTradesToCsv(List<Trade> trades) {
    try {
      // 現在の日時を取得
      LocalDateTime now = LocalDateTime.now();
      String dateStr = now.format(DATE_FORMATTER);
      String hourStr = String.format("%02d", now.getHour());

      // CSVファイル名の生成
      String tradesCsvName = String.format("trades_%s_%s.csv", dateStr, hourStr);
      Path tradesCsvFile = Paths.get(csvDir).resolve(tradesCsvName);

      // CSVファイルが存在しない場合はヘッダーを書き込む
      if (!Files.exists(tradesCsvFile)) {
        writeCsvHeaders(tradesCsvFile, null);
      }

      // データをCSVに追記
      try (BufferedWriter writer =
          Files.newBufferedWriter(
              tradesCsvFile, StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
        for (Trade trade : trades) {
          writer.write(
              String.format(
                  "%s,%s,%s,%s,%s,%s,%s,%s",
                  trade.getExchange(),
                  trade.getSymbol(),
                  trade.getTradeId(),
                  trade.getPrice().setScale(0, RoundingMode.HALF_UP),
                  trade.getSize().setScale(8, RoundingMode.HALF_UP),
                  trade.getSide(),
                  trade.getTimestamp(),
                  trade.getCreatedAt()));
          writer.newLine();
        }
        writer.flush(); // 明示的にフラッシュ
      }
      logger.info("Saved {} trades to {}", trades.size(), tradesCsvFile);
    } catch (IOException e) {
      logger.error("Error saving trades to CSV: {}", e.getMessage(), e);
    }
  }

  private void saveMarketBoardsToCsv(List<MarketBoard> boards) {
    try {
      // 現在の日時を取得
      LocalDateTime now = LocalDateTime.now();
      String dateStr = now.format(DATE_FORMATTER);
      String hourStr = String.format("%02d", now.getHour());

      // CSVファイル名の生成
      String marketBoardsCsvName = String.format("market_boards_%s_%s.csv", dateStr, hourStr);
      Path marketBoardsCsvFile = Paths.get(csvDir).resolve(marketBoardsCsvName);

      // CSVファイルが存在しない場合はヘッダーを書き込む
      if (!Files.exists(marketBoardsCsvFile)) {
        writeCsvHeaders(null, marketBoardsCsvFile);
      }

      // データをCSVに追記
      try (BufferedWriter writer =
          Files.newBufferedWriter(
              marketBoardsCsvFile, StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
        for (MarketBoard board : boards) {
          String[] csvRow = convertMarketBoardToCsvRow(board);
          writer.write(String.join(",", csvRow));
          writer.newLine();
        }
        writer.flush(); // 明示的にフラッシュ
      }
      logger.info("Saved {} market boards to {}", boards.size(), marketBoardsCsvFile);
    } catch (IOException e) {
      logger.error("Error saving market boards to CSV: {}", e.getMessage(), e);
    }
  }

  private String[] convertMarketBoardToCsvRow(MarketBoard board) {
    return new String[] {
      board.getExchange(),
      board.getSymbol(),
      board.getTs().toString(),
      // Get top 8 bids with volumes
      getPriceLevelString(board.getBids(), 0),
      getVolumeLevelString(board.getBids(), 0),
      getPriceLevelString(board.getBids(), 1),
      getVolumeLevelString(board.getBids(), 1),
      getPriceLevelString(board.getBids(), 2),
      getVolumeLevelString(board.getBids(), 2),
      getPriceLevelString(board.getBids(), 3),
      getVolumeLevelString(board.getBids(), 3),
      getPriceLevelString(board.getBids(), 4),
      getVolumeLevelString(board.getBids(), 4),
      getPriceLevelString(board.getBids(), 5),
      getVolumeLevelString(board.getBids(), 5),
      getPriceLevelString(board.getBids(), 6),
      getVolumeLevelString(board.getBids(), 6),
      getPriceLevelString(board.getBids(), 7),
      getVolumeLevelString(board.getBids(), 7),
      // Get top 8 asks with volumes
      getPriceLevelString(board.getAsks(), 0),
      getVolumeLevelString(board.getAsks(), 0),
      getPriceLevelString(board.getAsks(), 1),
      getVolumeLevelString(board.getAsks(), 1),
      getPriceLevelString(board.getAsks(), 2),
      getVolumeLevelString(board.getAsks(), 2),
      getPriceLevelString(board.getAsks(), 3),
      getVolumeLevelString(board.getAsks(), 3),
      getPriceLevelString(board.getAsks(), 4),
      getVolumeLevelString(board.getAsks(), 4),
      getPriceLevelString(board.getAsks(), 5),
      getVolumeLevelString(board.getAsks(), 5),
      getPriceLevelString(board.getAsks(), 6),
      getVolumeLevelString(board.getAsks(), 6),
      getPriceLevelString(board.getAsks(), 7),
      getVolumeLevelString(board.getAsks(), 7)
    };
  }

  private String getPriceLevelString(List<MarketBoard.PriceLevel> levels, int index) {
    if (levels == null || index >= levels.size()) {
      return "";
    }
    MarketBoard.PriceLevel level = levels.get(index);
    if (level == null || level.getPrice() == null) {
      return "";
    }
    return level.getPrice().setScale(0, RoundingMode.HALF_UP).toString();
  }

  private String getVolumeLevelString(List<MarketBoard.PriceLevel> levels, int index) {
    if (levels == null || index >= levels.size()) {
      return "";
    }
    MarketBoard.PriceLevel level = levels.get(index);
    if (level == null || level.getSize() == null) {
      return "";
    }
    return level.getSize().setScale(8, RoundingMode.HALF_UP).toString();
  }

  private void saveBestBidAskToCsv(List<BestBidAsk> bestBidAsks) {
    try {
      LocalDateTime now = LocalDateTime.now();
      String dateStr = now.format(DATE_FORMATTER);
      String hourStr = String.format("%02d", now.getHour());

      String bestBidAskCsvName = String.format("best_bid_ask_%s_%s.csv", dateStr, hourStr);
      Path bestBidAskCsvFile = Paths.get(csvDir).resolve(bestBidAskCsvName);

      if (!Files.exists(bestBidAskCsvFile)) {
        writeBestBidAskCsvHeaders(bestBidAskCsvFile);
      }

      try (BufferedWriter writer =
          Files.newBufferedWriter(
              bestBidAskCsvFile, StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
        for (BestBidAsk bestBidAsk : bestBidAsks) {
          writer.write(
              String.format(
                  "%s,%s,%s,%s,%s,%s,%s",
                  bestBidAsk.getExchange(),
                  bestBidAsk.getSymbol(),
                  bestBidAsk.getBestBid().setScale(0, RoundingMode.HALF_UP),
                  bestBidAsk.getBestBidVolume().setScale(8, RoundingMode.HALF_UP),
                  bestBidAsk.getBestAsk().setScale(0, RoundingMode.HALF_UP),
                  bestBidAsk.getBestAskVolume().setScale(8, RoundingMode.HALF_UP),
                  bestBidAsk.getTimestamp()));
          writer.newLine();
        }
        writer.flush();
      }
      logger.info("Saved {} best bid/ask records to {}", bestBidAsks.size(), bestBidAskCsvFile);
    } catch (IOException e) {
      logger.error("Error saving best bid/ask to CSV: {}", e.getMessage(), e);
    }
  }

  private void writeCsvHeaders(Path tradesCsvFile, Path marketBoardsCsvFile) throws IOException {
    // Trades CSVヘッダー
    if (tradesCsvFile != null && !Files.exists(tradesCsvFile)) {
      try (BufferedWriter writer = Files.newBufferedWriter(tradesCsvFile)) {
        writer.write("exchange,symbol,trade_id,price,size,side,timestamp,created_at");
        writer.newLine();
      }
    }

    // Market Boards CSVヘッダー
    if (marketBoardsCsvFile != null && !Files.exists(marketBoardsCsvFile)) {
      try (BufferedWriter writer = Files.newBufferedWriter(marketBoardsCsvFile)) {
        writer.write(
            "exchange,symbol,ts,bid1,bid1vol,bid2,bid2vol,bid3,bid3vol,bid4,bid4vol,"
                + "bid5,bid5vol,bid6,bid6vol,bid7,bid7vol,bid8,bid8vol,"
                + "ask1,ask1vol,ask2,ask2vol,ask3,ask3vol,ask4,ask4vol,"
                + "ask5,ask5vol,ask6,ask6vol,ask7,ask7vol,ask8,ask8vol");
        writer.newLine();
      }
    }
  }

  private void writeBestBidAskCsvHeaders(Path bestBidAskCsvFile) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(bestBidAskCsvFile)) {
      writer.write("exchange,symbol,best_bid,best_bid_volume,best_ask,best_ask_volume,timestamp");
      writer.newLine();
    }
  }

  @PreDestroy
  public void shutdown() {
    isRunning = false;
    workerExecutor.shutdown();
    try {
      if (!workerExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
        workerExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      workerExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
