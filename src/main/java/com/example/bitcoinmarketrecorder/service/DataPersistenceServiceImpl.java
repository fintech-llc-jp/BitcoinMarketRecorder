package com.example.bitcoinmarketrecorder.service;

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
  private final ExecutorService workerExecutor;
  private volatile boolean isRunning = true;

  @Value("${database.csv-dir:csv}")
  private String csvDir;

  public DataPersistenceServiceImpl() {
    this.tradeQueue = new LinkedBlockingQueue<>();
    this.boardQueue = new LinkedBlockingQueue<>();
    this.workerExecutor = Executors.newSingleThreadExecutor();
  }

  @PostConstruct
  public void initialize() {
    startWorker();
  }

  private void startWorker() {
    workerExecutor.submit(
        () -> {
          while (isRunning) {
            try {
              // Process trades
              List<Trade> trades = new ArrayList<>();
              tradeQueue.drainTo(trades, 100); // 最大100件までバッチ処理
              if (!trades.isEmpty()) {
                saveTradesToCsv(trades);
              }

              // Process market boards
              List<MarketBoard> boards = new ArrayList<>();
              boardQueue.drainTo(boards, 100);
              if (!boards.isEmpty()) {
                saveMarketBoardsToCsv(boards);
              }

              // キューが空の場合は少し待機
              if (trades.isEmpty() && boards.isEmpty()) {
                Thread.sleep(100);
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              break;
            } catch (Exception e) {
              logger.error("Error in worker thread: {}", e.getMessage(), e);
            }
          }
        });
  }

  @Override
  public void saveTrades(List<Trade> trades) {
    try {
      tradeQueue.addAll(trades);
    } catch (Exception e) {
      logger.error("Error adding trades to queue: {}", e.getMessage(), e);
    }
  }

  @Override
  public void saveMarketBoard(MarketBoard board) {
    try {
      boardQueue.add(board);
    } catch (Exception e) {
      logger.error("Error adding market board to queue: {}", e.getMessage(), e);
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
          Files.newBufferedWriter(tradesCsvFile, StandardOpenOption.APPEND)) {
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
      }
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
          Files.newBufferedWriter(marketBoardsCsvFile, StandardOpenOption.APPEND)) {
        for (MarketBoard board : boards) {
          String format =
              "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s";
          Object[] args =
              new Object[] {
                board.getExchange(),
                board.getSymbol(),
                board.getTs(),
                board.getBid1() != null ? board.getBid1().setScale(0, RoundingMode.HALF_UP) : "",
                board.getBid1vol() != null
                    ? board.getBid1vol().setScale(8, RoundingMode.HALF_UP)
                    : "",
                board.getBid2() != null ? board.getBid2().setScale(0, RoundingMode.HALF_UP) : "",
                board.getBid2vol() != null
                    ? board.getBid2vol().setScale(8, RoundingMode.HALF_UP)
                    : "",
                board.getBid3() != null ? board.getBid3().setScale(0, RoundingMode.HALF_UP) : "",
                board.getBid3vol() != null
                    ? board.getBid3vol().setScale(8, RoundingMode.HALF_UP)
                    : "",
                board.getBid4() != null ? board.getBid4().setScale(0, RoundingMode.HALF_UP) : "",
                board.getBid4vol() != null
                    ? board.getBid4vol().setScale(8, RoundingMode.HALF_UP)
                    : "",
                board.getBid5() != null ? board.getBid5().setScale(0, RoundingMode.HALF_UP) : "",
                board.getBid5vol() != null
                    ? board.getBid5vol().setScale(8, RoundingMode.HALF_UP)
                    : "",
                board.getBid6() != null ? board.getBid6().setScale(0, RoundingMode.HALF_UP) : "",
                board.getBid6vol() != null
                    ? board.getBid6vol().setScale(8, RoundingMode.HALF_UP)
                    : "",
                board.getBid7() != null ? board.getBid7().setScale(0, RoundingMode.HALF_UP) : "",
                board.getBid7vol() != null
                    ? board.getBid7vol().setScale(8, RoundingMode.HALF_UP)
                    : "",
                board.getBid8() != null ? board.getBid8().setScale(0, RoundingMode.HALF_UP) : "",
                board.getBid8vol() != null
                    ? board.getBid8vol().setScale(8, RoundingMode.HALF_UP)
                    : "",
                board.getAsk1() != null ? board.getAsk1().setScale(0, RoundingMode.HALF_UP) : "",
                board.getAsk1vol() != null
                    ? board.getAsk1vol().setScale(8, RoundingMode.HALF_UP)
                    : "",
                board.getAsk2() != null ? board.getAsk2().setScale(0, RoundingMode.HALF_UP) : "",
                board.getAsk2vol() != null
                    ? board.getAsk2vol().setScale(8, RoundingMode.HALF_UP)
                    : "",
                board.getAsk3() != null ? board.getAsk3().setScale(0, RoundingMode.HALF_UP) : "",
                board.getAsk3vol() != null
                    ? board.getAsk3vol().setScale(8, RoundingMode.HALF_UP)
                    : "",
                board.getAsk4() != null ? board.getAsk4().setScale(0, RoundingMode.HALF_UP) : "",
                board.getAsk4vol() != null
                    ? board.getAsk4vol().setScale(8, RoundingMode.HALF_UP)
                    : "",
                board.getAsk5() != null ? board.getAsk5().setScale(0, RoundingMode.HALF_UP) : "",
                board.getAsk5vol() != null
                    ? board.getAsk5vol().setScale(8, RoundingMode.HALF_UP)
                    : "",
                board.getAsk6() != null ? board.getAsk6().setScale(0, RoundingMode.HALF_UP) : "",
                board.getAsk6vol() != null
                    ? board.getAsk6vol().setScale(8, RoundingMode.HALF_UP)
                    : "",
                board.getAsk7() != null ? board.getAsk7().setScale(0, RoundingMode.HALF_UP) : "",
                board.getAsk7vol() != null
                    ? board.getAsk7vol().setScale(8, RoundingMode.HALF_UP)
                    : "",
                board.getAsk8() != null ? board.getAsk8().setScale(0, RoundingMode.HALF_UP) : ""
              };
          writer.write(String.format(format, args));
          writer.newLine();
        }
      }
    } catch (IOException e) {
      logger.error("Error saving market boards to CSV: {}", e.getMessage(), e);
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
