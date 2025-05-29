package com.example.bitcoinmarketrecorder.gmo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.bitcoinmarketrecorder.gmo.model.GmoOrderbook;
import com.example.bitcoinmarketrecorder.model.MarketBoard;
import com.example.bitcoinmarketrecorder.service.DataPersistenceService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class GmoWebSocketClientTest extends GmoWebSocketClient {

  @Mock private DataPersistenceService persistenceService;

  public GmoWebSocketClientTest() {
    super(null); // テスト用のコンストラクタ
  }

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testConvertToDomainMarketBoard() {
    // テストデータの準備
    GmoOrderbook orderbook = new GmoOrderbook();
    orderbook.setSymbol("BTC");

    // 10件のbidデータを作成（上位8件のみが使用されることを確認）
    List<GmoOrderbook.PriceLevel> bids = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      GmoOrderbook.PriceLevel level = new GmoOrderbook.PriceLevel();
      level.setPrice(new BigDecimal("15000000").add(new BigDecimal(i * 1000)));
      level.setSize(new BigDecimal("0.1").add(new BigDecimal(i * 0.01)));
      bids.add(level);
    }
    orderbook.setBids(bids);

    // 10件のaskデータを作成（上位8件のみが使用されることを確認）
    List<GmoOrderbook.PriceLevel> asks = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      GmoOrderbook.PriceLevel level = new GmoOrderbook.PriceLevel();
      level.setPrice(new BigDecimal("16000000").add(new BigDecimal(i * 1000)));
      level.setSize(new BigDecimal("0.1").add(new BigDecimal(i * 0.01)));
      asks.add(level);
    }
    orderbook.setAsks(asks);

    // 変換の実行
    MarketBoard marketBoard = convertToDomainMarketBoard(orderbook);

    // 検証
    assertNotNull(marketBoard);
    assertEquals("GMO", marketBoard.getExchange());
    assertEquals("BTC", marketBoard.getSymbol());
    assertNotNull(marketBoard.getTs());

    // bidsの検証
    assertNotNull(marketBoard.getBids());
    assertEquals(8, marketBoard.getBids().size());
    for (int i = 0; i < 8; i++) {
      MarketBoard.PriceLevel level = marketBoard.getBids().get(i);
      assertNotNull(level);
      assertNotNull(level.getPrice());
      assertNotNull(level.getSize());
      assertEquals(new BigDecimal("15000000").add(new BigDecimal(i * 1000)), level.getPrice());
      assertEquals(new BigDecimal("0.1").add(new BigDecimal(i * 0.01)), level.getSize());
    }

    // asksの検証
    assertNotNull(marketBoard.getAsks());
    assertEquals(8, marketBoard.getAsks().size());
    for (int i = 0; i < 8; i++) {
      MarketBoard.PriceLevel level = marketBoard.getAsks().get(i);
      assertNotNull(level);
      assertNotNull(level.getPrice());
      assertNotNull(level.getSize());
      assertEquals(new BigDecimal("16000000").add(new BigDecimal(i * 1000)), level.getPrice());
      assertEquals(new BigDecimal("0.1").add(new BigDecimal(i * 0.01)), level.getSize());
    }
  }

  @Test
  void testConvertToDomainMarketBoardWithEmptyData() {
    // 空のデータでテスト
    GmoOrderbook orderbook = new GmoOrderbook();
    orderbook.setSymbol("BTC");
    orderbook.setBids(new ArrayList<>());
    orderbook.setAsks(new ArrayList<>());

    // 変換の実行
    MarketBoard marketBoard = convertToDomainMarketBoard(orderbook);

    // 検証
    assertNotNull(marketBoard);
    assertEquals("GMO", marketBoard.getExchange());
    assertEquals("BTC", marketBoard.getSymbol());
    assertNotNull(marketBoard.getTs());
    assertNotNull(marketBoard.getBids());
    assertNotNull(marketBoard.getAsks());
    assertTrue(marketBoard.getBids().isEmpty());
    assertTrue(marketBoard.getAsks().isEmpty());
  }
}
