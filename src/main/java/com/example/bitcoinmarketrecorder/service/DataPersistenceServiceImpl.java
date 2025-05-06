package com.example.bitcoinmarketrecorder.service;

import com.example.bitcoinmarketrecorder.model.MarketBoard;
import com.example.bitcoinmarketrecorder.model.Trade;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataPersistenceServiceImpl implements DataPersistenceService {

  private static final Logger logger = LoggerFactory.getLogger(DataPersistenceServiceImpl.class);
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public DataPersistenceServiceImpl(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
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

  @Override
  @Transactional
  public void saveTrades(List<Trade> trades) {
    if (trades == null || trades.isEmpty()) {
      return;
    }

    String sql =
        "INSERT INTO trades (exchange, symbol, trade_id, price, size, side, timestamp, created_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON CONFLICT (trade_id) DO NOTHING"; // Avoid duplicates based on trade_id

    jdbcTemplate.batchUpdate(
        sql,
        new BatchPreparedStatementSetter() {
          @Override
          public void setValues(PreparedStatement ps, int i) throws SQLException {
            Trade trade = trades.get(i);
            ps.setString(1, trade.getExchange());
            ps.setString(2, trade.getSymbol());
            ps.setString(3, trade.getTradeId());
            // Convert BigDecimal to string to avoid scale issues
            ps.setString(4, trade.getPrice() != null ? trade.getPrice().toString() : null);
            ps.setString(5, trade.getSize() != null ? trade.getSize().toString() : null);
            ps.setString(6, trade.getSide());
            // Convert Instant to SQL Timestamp
            ps.setTimestamp(
                7, trade.getTimestamp() != null ? Timestamp.from(trade.getTimestamp()) : null);
            // Use current time for created_at if not set, otherwise use provided time
            ps.setTimestamp(
                8,
                trade.getCreatedAt() != null
                    ? Timestamp.valueOf(trade.getCreatedAt())
                    : Timestamp.valueOf(LocalDateTime.now()));
          }

          @Override
          public int getBatchSize() {
            return trades.size();
          }
        });
    // System.out.println("Saved " + trades.size() + " trades."); // Optional: logging
  }

  @Override
  @Transactional
  public void saveMarketBoard(MarketBoard board) {
    if (board == null) {
      return;
    }

    String sql =
        "INSERT INTO market_boards (exchange, symbol, ts, "
            + "bid1, bid1vol, bid2, bid2vol, bid3, bid3vol, bid4, bid4vol, "
            + "bid5, bid5vol, bid6, bid6vol, bid7, bid7vol, bid8, bid8vol, "
            + "ask1, ask1vol, ask2, ask2vol, ask3, ask3vol, ask4, ask4vol, "
            + "ask5, ask5vol, ask6, ask6vol, ask7, ask7vol, ask8, ask8vol) "
            + "VALUES (?, ?, ?, "
            + "?, ?, ?, ?, ?, ?, ?, ?, "
            + "?, ?, ?, ?, ?, ?, ?, ?, "
            + "?, ?, ?, ?, ?, ?, ?, ?, "
            + "?, ?, ?, ?, ?, ?, ?, ?)";

    jdbcTemplate.update(
        sql,
        board.getExchange(),
        board.getSymbol(),
        Timestamp.from(board.getTs()),
        // Bids - convert to string to avoid scale issues
        board.getBid1() != null ? board.getBid1().toString() : null,
        board.getBid1vol() != null ? board.getBid1vol().toString() : null,
        board.getBid2() != null ? board.getBid2().toString() : null,
        board.getBid2vol() != null ? board.getBid2vol().toString() : null,
        board.getBid3() != null ? board.getBid3().toString() : null,
        board.getBid3vol() != null ? board.getBid3vol().toString() : null,
        board.getBid4() != null ? board.getBid4().toString() : null,
        board.getBid4vol() != null ? board.getBid4vol().toString() : null,
        board.getBid5() != null ? board.getBid5().toString() : null,
        board.getBid5vol() != null ? board.getBid5vol().toString() : null,
        board.getBid6() != null ? board.getBid6().toString() : null,
        board.getBid6vol() != null ? board.getBid6vol().toString() : null,
        board.getBid7() != null ? board.getBid7().toString() : null,
        board.getBid7vol() != null ? board.getBid7vol().toString() : null,
        board.getBid8() != null ? board.getBid8().toString() : null,
        board.getBid8vol() != null ? board.getBid8vol().toString() : null,
        // Asks - convert to string to avoid scale issues
        board.getAsk1() != null ? board.getAsk1().toString() : null,
        board.getAsk1vol() != null ? board.getAsk1vol().toString() : null,
        board.getAsk2() != null ? board.getAsk2().toString() : null,
        board.getAsk2vol() != null ? board.getAsk2vol().toString() : null,
        board.getAsk3() != null ? board.getAsk3().toString() : null,
        board.getAsk3vol() != null ? board.getAsk3vol().toString() : null,
        board.getAsk4() != null ? board.getAsk4().toString() : null,
        board.getAsk4vol() != null ? board.getAsk4vol().toString() : null,
        board.getAsk5() != null ? board.getAsk5().toString() : null,
        board.getAsk5vol() != null ? board.getAsk5vol().toString() : null,
        board.getAsk6() != null ? board.getAsk6().toString() : null,
        board.getAsk6vol() != null ? board.getAsk6vol().toString() : null,
        board.getAsk7() != null ? board.getAsk7().toString() : null,
        board.getAsk7vol() != null ? board.getAsk7vol().toString() : null,
        board.getAsk8() != null ? board.getAsk8().toString() : null,
        board.getAsk8vol() != null ? board.getAsk8vol().toString() : null);
  }
}
