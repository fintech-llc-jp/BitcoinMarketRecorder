package com.example.bitcoinmarketrecorder.service;

import com.example.bitcoinmarketrecorder.model.MarketBoard;
import com.example.bitcoinmarketrecorder.model.Trade;
import jakarta.annotation.PreDestroy;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DataPersistenceServiceImpl implements DataPersistenceService {

  private static final Logger logger = LoggerFactory.getLogger(DataPersistenceServiceImpl.class);
  private final JdbcTemplate jdbcTemplate;
  private final BlockingQueue<Trade> tradeQueue;
  private final BlockingQueue<MarketBoard> boardQueue;
  private final ExecutorService workerExecutor;
  private volatile boolean isRunning = true;

  @Autowired
  public DataPersistenceServiceImpl(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.tradeQueue = new LinkedBlockingQueue<>();
    this.boardQueue = new LinkedBlockingQueue<>();
    this.workerExecutor = Executors.newSingleThreadExecutor();
    startWorker();
    createTablesIfNotExist();
  }

  private void createTablesIfNotExist() {
    try {
      // Create trades table if not exists
      jdbcTemplate.execute(
          """
          CREATE TABLE IF NOT EXISTS trades (
              exchange VARCHAR,
              symbol VARCHAR,
              trade_id VARCHAR PRIMARY KEY,
              price DECIMAL,
              size DECIMAL,
              side VARCHAR,
              timestamp TIMESTAMP,
              created_at TIMESTAMP
          )
          """);

      // Create market_boards table if not exists
      jdbcTemplate.execute(
          """
          CREATE TABLE IF NOT EXISTS market_boards (
              exchange VARCHAR,
              symbol VARCHAR,
              ts TIMESTAMP,
              bid1 DECIMAL,
              bid1vol DECIMAL,
              bid2 DECIMAL,
              bid2vol DECIMAL,
              bid3 DECIMAL,
              bid3vol DECIMAL,
              bid4 DECIMAL,
              bid4vol DECIMAL,
              bid5 DECIMAL,
              bid5vol DECIMAL,
              bid6 DECIMAL,
              bid6vol DECIMAL,
              bid7 DECIMAL,
              bid7vol DECIMAL,
              bid8 DECIMAL,
              bid8vol DECIMAL,
              ask1 DECIMAL,
              ask1vol DECIMAL,
              ask2 DECIMAL,
              ask2vol DECIMAL,
              ask3 DECIMAL,
              ask3vol DECIMAL,
              ask4 DECIMAL,
              ask4vol DECIMAL,
              ask5 DECIMAL,
              ask5vol DECIMAL,
              ask6 DECIMAL,
              ask6vol DECIMAL,
              ask7 DECIMAL,
              ask7vol DECIMAL,
              ask8 DECIMAL,
              ask8vol DECIMAL
          )
          """);

      logger.info("Database tables created successfully");
    } catch (Exception e) {
      logger.error("Failed to create database tables: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to create database tables", e);
    }
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
                saveTradesToDb(trades);
              }

              // Process market boards
              List<MarketBoard> boards = new ArrayList<>();
              boardQueue.drainTo(boards, 100);
              if (!boards.isEmpty()) {
                saveMarketBoardsToDb(boards);
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

  private void saveTradesToDb(List<Trade> trades) {
    try {
      jdbcTemplate.batchUpdate(
          "INSERT INTO trades (exchange, symbol, trade_id, price, size, side, timestamp,"
              + " created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (trade_id) DO NOTHING",
          trades,
          100,
          (ps, trade) -> {
            ps.setString(1, trade.getExchange());
            ps.setString(2, trade.getSymbol());
            ps.setString(3, trade.getTradeId());
            ps.setBigDecimal(4, trade.getPrice());
            ps.setBigDecimal(5, trade.getSize());
            ps.setString(6, trade.getSide());
            ps.setTimestamp(7, Timestamp.from(trade.getTimestamp()));
            ps.setTimestamp(8, Timestamp.valueOf(trade.getCreatedAt()));
          });
    } catch (Exception e) {
      logger.error("Error saving trades to database: {}", e.getMessage(), e);
    }
  }

  private void saveMarketBoardsToDb(List<MarketBoard> boards) {
    try {
      jdbcTemplate.batchUpdate(
          "INSERT INTO market_boards (exchange, symbol, ts, bid1, bid1vol, bid2, bid2vol, bid3,"
              + " bid3vol, bid4, bid4vol, bid5, bid5vol, bid6, bid6vol, bid7, bid7vol, bid8,"
              + " bid8vol, ask1, ask1vol, ask2, ask2vol, ask3, ask3vol, ask4, ask4vol, ask5,"
              + " ask5vol, ask6, ask6vol, ask7, ask7vol, ask8, ask8vol) VALUES (?, ?, ?, ?, ?, ?,"
              + " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
              + " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          boards,
          100,
          (ps, board) -> {
            int idx = 1;
            ps.setString(idx++, board.getExchange());
            ps.setString(idx++, board.getSymbol());
            ps.setTimestamp(idx++, Timestamp.from(board.getTs()));
            ps.setBigDecimal(idx++, board.getBid1());
            ps.setBigDecimal(idx++, board.getBid1vol());
            ps.setBigDecimal(idx++, board.getBid2());
            ps.setBigDecimal(idx++, board.getBid2vol());
            ps.setBigDecimal(idx++, board.getBid3());
            ps.setBigDecimal(idx++, board.getBid3vol());
            ps.setBigDecimal(idx++, board.getBid4());
            ps.setBigDecimal(idx++, board.getBid4vol());
            ps.setBigDecimal(idx++, board.getBid5());
            ps.setBigDecimal(idx++, board.getBid5vol());
            ps.setBigDecimal(idx++, board.getBid6());
            ps.setBigDecimal(idx++, board.getBid6vol());
            ps.setBigDecimal(idx++, board.getBid7());
            ps.setBigDecimal(idx++, board.getBid7vol());
            ps.setBigDecimal(idx++, board.getBid8());
            ps.setBigDecimal(idx++, board.getBid8vol());
            ps.setBigDecimal(idx++, board.getAsk1());
            ps.setBigDecimal(idx++, board.getAsk1vol());
            ps.setBigDecimal(idx++, board.getAsk2());
            ps.setBigDecimal(idx++, board.getAsk2vol());
            ps.setBigDecimal(idx++, board.getAsk3());
            ps.setBigDecimal(idx++, board.getAsk3vol());
            ps.setBigDecimal(idx++, board.getAsk4());
            ps.setBigDecimal(idx++, board.getAsk4vol());
            ps.setBigDecimal(idx++, board.getAsk5());
            ps.setBigDecimal(idx++, board.getAsk5vol());
            ps.setBigDecimal(idx++, board.getAsk6());
            ps.setBigDecimal(idx++, board.getAsk6vol());
            ps.setBigDecimal(idx++, board.getAsk7());
            ps.setBigDecimal(idx++, board.getAsk7vol());
            ps.setBigDecimal(idx++, board.getAsk8());
            ps.setBigDecimal(idx, board.getAsk8vol());
          });
    } catch (Exception e) {
      logger.error("Error saving market boards to database: {}", e.getMessage(), e);
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
