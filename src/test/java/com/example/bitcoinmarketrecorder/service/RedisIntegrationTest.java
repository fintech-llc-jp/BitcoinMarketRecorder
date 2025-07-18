package com.example.bitcoinmarketrecorder.service;

import static org.junit.jupiter.api.Assertions.*;

import com.example.bitcoinmarketrecorder.config.RedisConfig;
import com.example.bitcoinmarketrecorder.config.RedisPublisherProperties;
import com.example.bitcoinmarketrecorder.model.MarketBoard;
import com.example.bitcoinmarketrecorder.model.Trade;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
    classes = {RedisConfig.class, RedisPublisherService.class, RedisPublisherProperties.class})
@TestPropertySource(
    properties = {
      "redis.host=localhost",
      "redis.port=6379",
      "redis.database=1", // Use different database for tests
      "redis.publisher.enabled=true",
      "redis.publisher.market-make.channel-prefix=test-market-make",
      "redis.publisher.trade-insert.channel-prefix=test-trade-insert"
    })
class RedisIntegrationTest {

  @Autowired private RedisPublisherService redisPublisherService;

  @Autowired private RedisTemplate<String, Object> redisTemplate;

  @Autowired private RedisPublisherProperties redisPublisherProperties;

  @Test
  void testRedisConnectionHealthy() {
    // Test basic Redis connectivity
    assertTrue(redisPublisherService.isHealthy(), "Redis should be healthy and accessible");
  }

  @Test
  void testTradeInsertPublishing() throws Exception {
    // Given
    String symbol = "BTC_JPY";
    Trade trade = new Trade();
    trade.setExchange("GMO");
    trade.setSymbol(symbol);
    trade.setTradeId("12345");
    trade.setPrice(new BigDecimal("5000000"));
    trade.setSize(new BigDecimal("0.1"));
    trade.setSide("BUY");
    ZoneId zoneId = ZoneId.systemDefault();
    trade.setTimestamp(LocalDateTime.now().atZone(zoneId).toInstant());

    ExchSimService.TradeInsertRequest request = new ExchSimService.TradeInsertRequest();
    request.setSymbol(symbol);
    request.setPrice(trade.getPrice().doubleValue());
    request.setQuantity(trade.getSize().doubleValue());
    request.setSide(trade.getSide());

    // Setup listener to capture published message
    CountDownLatch latch = new CountDownLatch(1);
    final String[] receivedMessage = new String[1];

    String channel = redisPublisherProperties.getTradeInsert().getChannelName(symbol);

    redisTemplate
        .getConnectionFactory()
        .getConnection()
        .subscribe(
            (message, pattern) -> {
              receivedMessage[0] = new String(message.getBody());
              latch.countDown();
            },
            channel.getBytes());

    // When
    redisPublisherService.publishTradeInsertSync(symbol, request);

    // Then
    assertTrue(latch.await(5, TimeUnit.SECONDS), "Should receive message within 5 seconds");
    assertNotNull(receivedMessage[0], "Should receive a message");
    String line = receivedMessage[0];
    // エスケープされたJSON文字列を正しくアンエスケープしてからパース
    String jsonContent = line.substring(1, line.length() - 1).replace("\\\"", "\"");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(jsonContent);
    assertEquals(symbol, node.get("symbol").asText());
    assertEquals(5000000.0, node.get("price").asDouble(), "Message should contain correct price");
    assertEquals("BUY", node.get("side").asText(), "Message should contain correct side");
  }

  @Test
  void testMarketMakePublishing() throws Exception {
    // Given
    String symbol = "BTC_JPY";
    MarketBoard marketBoard = new MarketBoard();
    marketBoard.setExchange("GMO");
    marketBoard.setSymbol(symbol);
    ZoneId zoneId = ZoneId.systemDefault();
    marketBoard.setTs(LocalDateTime.now().atZone(zoneId).toInstant());

    List<MarketBoard.PriceLevel> bids = new ArrayList<>();
    bids.add(new MarketBoard.PriceLevel(new BigDecimal("4999000"), new BigDecimal("0.5")));
    bids.add(new MarketBoard.PriceLevel(new BigDecimal("4998000"), new BigDecimal("1.0")));
    marketBoard.setBids(bids);

    List<MarketBoard.PriceLevel> asks = new ArrayList<>();
    asks.add(new MarketBoard.PriceLevel(new BigDecimal("5001000"), new BigDecimal("0.3")));
    asks.add(new MarketBoard.PriceLevel(new BigDecimal("5002000"), new BigDecimal("0.8")));
    marketBoard.setAsks(asks);

    ExchSimService.MarketMakeRequest request = new ExchSimService.MarketMakeRequest();
    request.setSymbol(symbol);

    List<ExchSimService.PriceLevel> bidLevels = new ArrayList<>();
    List<ExchSimService.PriceLevel> askLevels = new ArrayList<>();

    for (MarketBoard.PriceLevel bid : marketBoard.getBids()) {
      bidLevels.add(
          new ExchSimService.PriceLevel(bid.getPrice().doubleValue(), bid.getSize().doubleValue()));
    }

    for (MarketBoard.PriceLevel ask : marketBoard.getAsks()) {
      askLevels.add(
          new ExchSimService.PriceLevel(ask.getPrice().doubleValue(), ask.getSize().doubleValue()));
    }

    request.setBidLevels(bidLevels);
    request.setAskLevels(askLevels);

    // Setup listener to capture published message
    CountDownLatch latch = new CountDownLatch(1);
    final String[] receivedMessage = new String[1];

    String channel = redisPublisherProperties.getMarketMake().getChannelName(symbol);

    redisTemplate
        .getConnectionFactory()
        .getConnection()
        .subscribe(
            (message, pattern) -> {
              receivedMessage[0] = new String(message.getBody());
              latch.countDown();
            },
            channel.getBytes());

    // When
    redisPublisherService.publishMarketMakeSync(symbol, request);

    // Then
    assertTrue(latch.await(5, TimeUnit.SECONDS), "Should receive message within 5 seconds");
    assertNotNull(receivedMessage[0], "Should receive a message");

    // Parse JSON and validate structure
    String jsonContent =
        receivedMessage[0].substring(1, receivedMessage[0].length() - 1).replace("\\\"", "\"");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(jsonContent);

    assertEquals(symbol, node.get("symbol").asText(), "Message should contain correct symbol");
    assertTrue(node.has("bidLevels"), "Message should contain bid levels");
    assertTrue(node.has("askLevels"), "Message should contain ask levels");

    // Validate bid levels structure
    JsonNode bidLevelsNode = node.get("bidLevels");
    assertTrue(bidLevelsNode.isArray(), "bidLevels should be an array");
    assertEquals(2, bidLevelsNode.size(), "Should have 2 bid levels");

    // Validate ask levels structure
    JsonNode askLevelsNode = node.get("askLevels");
    assertTrue(askLevelsNode.isArray(), "askLevels should be an array");
    assertEquals(2, askLevelsNode.size(), "Should have 2 ask levels");
  }

  @Test
  void testPublishingDisabled() {
    // Given - temporarily disable publishing
    redisPublisherProperties.setEnabled(false);

    String symbol = "BTC_JPY";
    ExchSimService.TradeInsertRequest request = new ExchSimService.TradeInsertRequest();
    request.setSymbol(symbol);

    // When - this should not throw any errors even when disabled
    assertDoesNotThrow(
        () -> {
          redisPublisherService.publishTradeInsertSync(symbol, request);
        });

    // Restore state
    redisPublisherProperties.setEnabled(true);
  }

  @Test
  void testChannelNaming() {
    // Test that channels are named correctly according to configuration
    String symbol = "BTC_JPY";

    String tradeChannel = redisPublisherProperties.getTradeInsert().getChannelName(symbol);
    String marketChannel = redisPublisherProperties.getMarketMake().getChannelName(symbol);

    boolean fuck = tradeChannel.startsWith("trade-insert:");
    System.out.println(fuck);

    assertTrue(
        tradeChannel.startsWith("trade-insert:"), "Trade channel should have correct prefix");
    assertTrue(tradeChannel.endsWith(symbol), "Trade channel should end with symbol");

    assertTrue(
        marketChannel.startsWith("market-make:"), "Market channel should have correct prefix");
    assertTrue(marketChannel.endsWith(symbol), "Market channel should end with symbol");
  }
}
