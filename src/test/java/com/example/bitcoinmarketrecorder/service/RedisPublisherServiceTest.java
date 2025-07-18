package com.example.bitcoinmarketrecorder.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.bitcoinmarketrecorder.config.RedisPublisherProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class RedisPublisherServiceTest {

  @Mock private RedisTemplate<String, Object> redisTemplate;

  @Mock private RedisPublisherProperties redisPublisherProperties;

  @InjectMocks private RedisPublisherService redisPublisherService;

  private ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testPublishTradeInsertSync_Success() {
    // Given
    when(redisPublisherProperties.isEnabled()).thenReturn(true);
    RedisPublisherProperties.TradeInsert tradeInsert = new RedisPublisherProperties.TradeInsert();
    when(redisPublisherProperties.getTradeInsert()).thenReturn(tradeInsert);

    String symbol = "BTC_JPY";
    ExchSimService.TradeInsertRequest request = new ExchSimService.TradeInsertRequest();
    request.setSymbol(symbol);
    request.setPrice(5000000.0);
    request.setQuantity(0.1);
    request.setSide("BUY");

    String expectedChannel = "trade-insert:" + symbol;

    // When
    redisPublisherService.publishTradeInsertSync(symbol, request);

    // Then
    ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

    verify(redisTemplate).convertAndSend(channelCaptor.capture(), messageCaptor.capture());

    assertEquals(expectedChannel, channelCaptor.getValue());

    // Verify the message content
    String capturedMessage = messageCaptor.getValue();
    assertTrue(capturedMessage.contains("\"symbol\":\"" + symbol + "\""));
    assertTrue(capturedMessage.contains("\"price\":5000000.0"));
    assertTrue(capturedMessage.contains("\"quantity\":0.1"));
    assertTrue(capturedMessage.contains("\"side\":\"BUY\""));
  }

  @Test
  void testPublishMarketMakeSync_Success() {
    // Given
    when(redisPublisherProperties.isEnabled()).thenReturn(true);
    RedisPublisherProperties.MarketMake marketMake = new RedisPublisherProperties.MarketMake();
    when(redisPublisherProperties.getMarketMake()).thenReturn(marketMake);

    String symbol = "BTC_JPY";
    ExchSimService.MarketMakeRequest request = new ExchSimService.MarketMakeRequest();
    request.setSymbol(symbol);

    String expectedChannel = "market-make:" + symbol;

    // When
    redisPublisherService.publishMarketMakeSync(symbol, request);

    // Then
    ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

    verify(redisTemplate).convertAndSend(channelCaptor.capture(), messageCaptor.capture());

    assertEquals(expectedChannel, channelCaptor.getValue());

    // Verify the message content
    String capturedMessage = messageCaptor.getValue();
    assertTrue(capturedMessage.contains("\"symbol\":\"" + symbol + "\""));
  }

  @Test
  void testPublishTradeInsertSync_Disabled() {
    // Given
    when(redisPublisherProperties.isEnabled()).thenReturn(false);

    String symbol = "BTC_JPY";
    ExchSimService.TradeInsertRequest request = new ExchSimService.TradeInsertRequest();

    // When
    redisPublisherService.publishTradeInsertSync(symbol, request);

    // Then
    verify(redisTemplate, never()).convertAndSend(any(), any());
  }

  @Test
  void testPublishMarketMakeSync_Disabled() {
    // Given
    when(redisPublisherProperties.isEnabled()).thenReturn(false);

    String symbol = "BTC_JPY";
    ExchSimService.MarketMakeRequest request = new ExchSimService.MarketMakeRequest();

    // When
    redisPublisherService.publishMarketMakeSync(symbol, request);

    // Then
    verify(redisTemplate, never()).convertAndSend(any(), any());
  }

  @Test
  void testIsHealthy_Success() {
    // Given
    when(redisTemplate.hasKey(any())).thenReturn(true);

    // When
    boolean result = redisPublisherService.isHealthy();

    // Then
    assertTrue(result);
    verify(redisTemplate).hasKey("health-check");
  }

  @Test
  void testIsHealthy_Exception() {
    // Given
    when(redisTemplate.hasKey(any())).thenThrow(new RuntimeException("Redis connection failed"));

    // When
    boolean result = redisPublisherService.isHealthy();

    // Then
    assertFalse(result);
  }

  @Test
  void testPublishTradeInsertSync_SerializationError() {
    // Given - create a request that will cause serialization issues
    when(redisPublisherProperties.isEnabled()).thenReturn(true);
    RedisPublisherProperties.TradeInsert tradeInsert = new RedisPublisherProperties.TradeInsert();
    when(redisPublisherProperties.getTradeInsert()).thenReturn(tradeInsert);

    String symbol = "BTC_JPY";
    ExchSimService.TradeInsertRequest request =
        new ExchSimService.TradeInsertRequest() {
          @Override
          public String getSymbol() {
            throw new RuntimeException("Serialization error");
          }
        };

    // When & Then
    assertThrows(
        RuntimeException.class,
        () -> {
          redisPublisherService.publishTradeInsertSync(symbol, request);
        });
  }

  @Test
  void testPublishTradeInsertSync_RedisError() {
    // Given
    when(redisPublisherProperties.isEnabled()).thenReturn(true);
    RedisPublisherProperties.TradeInsert tradeInsert = new RedisPublisherProperties.TradeInsert();
    when(redisPublisherProperties.getTradeInsert()).thenReturn(tradeInsert);

    String symbol = "BTC_JPY";
    ExchSimService.TradeInsertRequest request = new ExchSimService.TradeInsertRequest();
    request.setSymbol(symbol);

    doThrow(new RuntimeException("Redis publish failed"))
        .when(redisTemplate)
        .convertAndSend(any(), any());

    // When & Then
    assertThrows(
        RuntimeException.class,
        () -> {
          redisPublisherService.publishTradeInsertSync(symbol, request);
        });
  }
}
