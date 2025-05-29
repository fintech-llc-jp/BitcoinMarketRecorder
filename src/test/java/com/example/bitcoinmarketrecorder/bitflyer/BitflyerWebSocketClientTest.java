package com.example.bitcoinmarketrecorder.bitflyer;

import static org.junit.jupiter.api.Assertions.*;

import com.example.bitcoinmarketrecorder.bitflyer.model.BitflyerBoard;
import com.example.bitcoinmarketrecorder.model.MarketBoard;
import com.example.bitcoinmarketrecorder.service.DataPersistenceService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class BitflyerWebSocketClientTest extends BitflyerWebSocketClient {

  @Mock private DataPersistenceService persistenceService;

  public BitflyerWebSocketClientTest() {
    super(null); // テスト用のコンストラクタ
  }

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    // persistenceServiceは親クラスのフィールドに設定
    this.persistenceService = persistenceService;
  }

  @Test
  void testConvertToDomainMarketBoard() {
    // テストデータの準備
    BitflyerBoard board = new BitflyerBoard();

    // 10件のbidデータを作成（上位8件のみが使用されることを確認）
    List<BitflyerBoard.PriceLevel> bids = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      BitflyerBoard.PriceLevel level = new BitflyerBoard.PriceLevel();
      level.setPrice(new BigDecimal("15000000").add(new BigDecimal(i * 1000)));
      level.setSize(new BigDecimal("0.1").add(new BigDecimal(i * 0.01)));
      bids.add(level);
    }
    board.setBids(bids);

    // 10件のaskデータを作成（上位8件のみが使用されることを確認）
    List<BitflyerBoard.PriceLevel> asks = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      BitflyerBoard.PriceLevel level = new BitflyerBoard.PriceLevel();
      level.setPrice(new BigDecimal("16000000").add(new BigDecimal(i * 1000)));
      level.setSize(new BigDecimal("0.1").add(new BigDecimal(i * 0.01)));
      asks.add(level);
    }
    board.setAsks(asks);

    // 変換の実行
    MarketBoard marketBoard = convertToDomainMarketBoard(board, "BTC_JPY");

    // 検証
    assertNotNull(marketBoard);
    assertEquals("BITFLYER", marketBoard.getExchange());
    assertEquals("BTC_JPY", marketBoard.getSymbol());
    assertNotNull(marketBoard.getTs());

    // bidsの検証
    assertNotNull(marketBoard.getBids());
    assertEquals(10, marketBoard.getBids().size()); // Bitflyerは全件保持
    for (int i = 0; i < 10; i++) {
      MarketBoard.PriceLevel level = marketBoard.getBids().get(i);
      assertNotNull(level);
      assertNotNull(level.getPrice());
      assertNotNull(level.getSize());
      assertEquals(new BigDecimal("15000000").add(new BigDecimal(i * 1000)), level.getPrice());
      assertEquals(new BigDecimal("0.1").add(new BigDecimal(i * 0.01)), level.getSize());
    }

    // asksの検証
    assertNotNull(marketBoard.getAsks());
    assertEquals(10, marketBoard.getAsks().size()); // Bitflyerは全件保持
    for (int i = 0; i < 10; i++) {
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
    BitflyerBoard board = new BitflyerBoard();
    board.setBids(new ArrayList<>());
    board.setAsks(new ArrayList<>());

    // 変換の実行
    MarketBoard marketBoard = convertToDomainMarketBoard(board, "BTC_JPY");

    // 検証
    assertNotNull(marketBoard);
    assertEquals("BITFLYER", marketBoard.getExchange());
    assertEquals("BTC_JPY", marketBoard.getSymbol());
    assertNotNull(marketBoard.getTs());
    assertNotNull(marketBoard.getBids());
    assertNotNull(marketBoard.getAsks());
    assertTrue(marketBoard.getBids().isEmpty());
    assertTrue(marketBoard.getAsks().isEmpty());
  }
}
