package com.example.bitcoinmarketrecorder.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DatabaseBackupService {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseBackupService.class);
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
  private final ReentrantLock backupLock = new ReentrantLock();

  @Value("${database.csv-dir:csv}")
  private String csvDir;

  @Autowired private DataPersistenceService dataPersistenceService;

  // 手動実行用のメソッド
  public void manualBackup() {
    logger.info("Starting manual backup...");
    backupAndCleanup();
    logger.info("Manual backup completed");
  }

  @Scheduled(cron = "0 0 * * * ?") // 毎時0分に実行
  public void backupAndCleanup() {
    if (!backupLock.tryLock()) {
      logger.warn("Another backup operation is in progress, skipping this backup");
      return;
    }

    try {
      // CSVディレクトリの作成
      Path csvPath = Paths.get(csvDir);
      if (!Files.exists(csvPath)) {
        Files.createDirectories(csvPath);
      }

      // 現在の日時を取得
      LocalDateTime now = LocalDateTime.now();
      String dateStr = now.format(DATE_FORMATTER);
      String hourStr = String.format("%02d", now.getHour());

      // CSVファイル名の生成
      String tradesCsvName = String.format("trades_%s_%s.csv", dateStr, hourStr);
      String marketBoardsCsvName = String.format("market_boards_%s_%s.csv", dateStr, hourStr);
      Path tradesCsvFile = csvPath.resolve(tradesCsvName);
      Path marketBoardsCsvFile = csvPath.resolve(marketBoardsCsvName);

      // CSVファイルのヘッダーを書き込み
      writeCsvHeaders(tradesCsvFile, marketBoardsCsvFile);
      logger.info("CSV files created with headers: {} and {}", tradesCsvFile, marketBoardsCsvFile);

    } catch (IOException e) {
      logger.error("Error during backup and cleanup: {}", e.getMessage(), e);
    } finally {
      backupLock.unlock();
    }
  }

  private void writeCsvHeaders(Path tradesCsvFile, Path marketBoardsCsvFile) throws IOException {
    // Trades CSVヘッダー
    try (BufferedWriter writer = Files.newBufferedWriter(tradesCsvFile)) {
      writer.write("exchange,symbol,trade_id,price,size,side,timestamp,created_at");
      writer.newLine();
    }

    // Market Boards CSVヘッダー
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
