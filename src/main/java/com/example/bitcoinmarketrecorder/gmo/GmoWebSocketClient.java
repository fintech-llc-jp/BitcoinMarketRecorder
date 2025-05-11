package com.example.bitcoinmarketrecorder.gmo;

import com.example.bitcoinmarketrecorder.gmo.model.GmoOrderbook;
import com.example.bitcoinmarketrecorder.gmo.model.GmoTradeRecord;
import com.example.bitcoinmarketrecorder.model.MarketBoard;
import com.example.bitcoinmarketrecorder.model.Trade;
import com.example.bitcoinmarketrecorder.service.DataPersistenceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

@Component
public class GmoWebSocketClient {

  private static final Logger logger = LoggerFactory.getLogger(GmoWebSocketClient.class);

  @Value("${gmo.api.ws-url}")
  private String wsUrl;

  private final WebSocketClient client = new ReactorNettyWebSocketClient();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final DataPersistenceService persistenceService;
  private Disposable connectionDisposable;
  private final AtomicBoolean isReconnecting = new AtomicBoolean(false);

  // Symbols defined in README.md (GMO versions)
  private static final String SYMBOL_BTC_SPOT = "BTC"; // 現物
  private static final String SYMBOL_BTC_FX = "BTC_JPY"; // レバレッジ取引

  // GMO channel names
  private static final String CHANNEL_ORDERBOOK = "orderbooks";
  private static final String CHANNEL_TRADES = "trades";

  @Autowired
  public GmoWebSocketClient(DataPersistenceService persistenceService) {
    this.persistenceService = persistenceService;
  }

  @PostConstruct
  public void connect() {
    if (connectionDisposable == null || connectionDisposable.isDisposed()) {
      logger.info("Connecting to GMO WebSocket: {}", wsUrl);
      connectionDisposable =
          client
              .execute(
                  URI.create(wsUrl),
                  session -> {
                    logger.info("GMO WebSocket session established");
                    // サブスクリプションメッセージの送信間隔を2秒に設定
                    Flux<WebSocketMessage> subscriptionMessages =
                        Flux.concat(
                            createSubscriptionMessageMono(
                                    session, CHANNEL_ORDERBOOK, SYMBOL_BTC_SPOT)
                                .delayElement(Duration.ofSeconds(2)),
                            createSubscriptionMessageMono(session, CHANNEL_TRADES, SYMBOL_BTC_SPOT)
                                .delayElement(Duration.ofSeconds(2)),
                            createSubscriptionMessageMono(session, CHANNEL_ORDERBOOK, SYMBOL_BTC_FX)
                                .delayElement(Duration.ofSeconds(2)),
                            createSubscriptionMessageMono(session, CHANNEL_TRADES, SYMBOL_BTC_FX)
                                .delayElement(Duration.ofSeconds(2)));

                    // Send messages and then start receiving
                    return session
                        .send(subscriptionMessages)
                        .thenMany(
                            session
                                .receive()
                                .map(WebSocketMessage::getPayloadAsText)
                                .doOnNext(msg -> logger.info("Raw message received: {}", msg))
                                .publishOn(Schedulers.boundedElastic())
                                .flatMap(this::handleMessage)
                                .onErrorResume(
                                    e -> {
                                      logger.error(
                                          "Error processing GMO WebSocket message: {}",
                                          e.getMessage(),
                                          e);
                                      reconnect();
                                      return Mono.empty();
                                    }))
                        .then();
                  })
              .retryWhen(
                  Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(5))
                      .maxBackoff(Duration.ofMinutes(1)))
              .doOnError(
                  error -> {
                    logger.error("GMO WebSocket connection error: {}", error.getMessage());
                    reconnect();
                  })
              .doFinally(
                  signalType -> {
                    logger.warn("GMO WebSocket connection closed: {}", signalType);
                    reconnect();
                  })
              .subscribe(
                  null,
                  error ->
                      logger.error(
                          "GMO WebSocket connection failed permanently: {}", error.getMessage()),
                  () -> logger.info("GMO WebSocket connection sequence completed."));
    }
  }

  @PreDestroy
  public void disconnect() {
    if (connectionDisposable != null && !connectionDisposable.isDisposed()) {
      logger.info("Disconnecting from GMO WebSocket.");
      connectionDisposable.dispose();
    }
  }

  private Mono<WebSocketMessage> createSubscriptionMessageMono(
      org.springframework.web.reactive.socket.WebSocketSession session,
      String channel,
      String symbol) {
    try {
      // GMOのWebSocket APIの仕様に合わせてメッセージを構築
      Map<String, Object> messageMap =
          Map.of(
              "command",
              "subscribe",
              "channel",
              channel,
              "symbol",
              symbol.toUpperCase() // シンボルを大文字に変換
              );
      String jsonMessage = objectMapper.writeValueAsString(messageMap);
      // logger.info(
      //     "Sending GMO subscription message - Channel: {}, Symbol: {}, Message: {}",
      //     channel,
      //     symbol,
      //     jsonMessage);
      return Mono.just(session.textMessage(jsonMessage));
    } catch (JsonProcessingException e) {
      // logger.error(
      //     "Failed to create GMO subscription message for channel: {}, symbol: {}, error: {}",
      //     channel,
      //     symbol,
      //     e.getMessage());
      return Mono.error(e);
    }
  }

  private Mono<Void> handleMessage(String jsonPayload) {
    try {
      logger.info("Processing GMO message: {}", jsonPayload);
      JsonNode rootNode = objectMapper.readTree(jsonPayload);

      // チャンネルメッセージの処理
      if (rootNode.has("channel")) {
        String channel = rootNode.get("channel").asText();
        // logger.info("Processing GMO channel message - Channel: {}", channel);

        if (CHANNEL_TRADES.equals(channel)) {
          // Single trade object
          GmoTradeRecord trade = objectMapper.convertValue(rootNode, GmoTradeRecord.class);
          Trade domainTrade = convertToDomainTrade(trade);
          logger.info("Received GMO trade: {}", trade);
          persistenceService.saveTrades(List.of(domainTrade));
        } else if (CHANNEL_ORDERBOOK.equals(channel)) {
          GmoOrderbook orderbook = objectMapper.convertValue(rootNode, GmoOrderbook.class);
          MarketBoard marketBoard = convertToDomainMarketBoard(orderbook);
          logger.info("Received GMO MarketBoard for symbol: {}", orderbook.getSymbol());
          persistenceService.saveMarketBoard(marketBoard);
        } else {
          logger.debug("Received unhandled channel message: {}", channel);
        }
      } else if (rootNode.has("result")) {
        logger.info("Received subscription confirmation: {}", rootNode.get("result"));
      } else if (rootNode.has("error")) {
        logger.error("Received error from GMO: {}", rootNode.get("error"));
        reconnect();
      } else {
        logger.info("Received other message: {}", jsonPayload);
      }
    } catch (Exception e) {
      logger.error("Error handling GMO message: {}", jsonPayload, e);
      reconnect();
    }
    return Mono.empty();
  }

  private void reconnect() {
    if (isReconnecting.compareAndSet(false, true)) {
      logger.info("Attempting to reconnect to GMO WebSocket...");
      if (connectionDisposable != null && !connectionDisposable.isDisposed()) {
        connectionDisposable.dispose();
      }
      // 5秒待ってから再接続を試みる
      Mono.delay(Duration.ofSeconds(5))
          .subscribe(
              delay -> {
                connect();
                isReconnecting.set(false);
                logger.info("Reconnection attempt completed");
              },
              error -> {
                logger.error("Failed to schedule reconnection: {}", error.getMessage());
                isReconnecting.set(false);
              });
    } else {
      logger.debug("Reconnection already in progress, skipping...");
    }
  }

  private Trade convertToDomainTrade(GmoTradeRecord gmoTrade) {
    Trade trade = new Trade();
    trade.setExchange("GMO");
    // シンボル名を統一
    trade.setSymbol(gmoTrade.getSymbol().equals("BTC") ? "BTC_JPY" : gmoTrade.getSymbol());
    // executionIdがnullの場合は現在時刻を使用
    String executionId = gmoTrade.getExecutionId();
    if (executionId == null || executionId.isEmpty()) {
      executionId = String.valueOf(System.currentTimeMillis());
    }
    trade.setTradeId("GMO-" + executionId);
    trade.setPrice(gmoTrade.getPrice());
    trade.setSize(gmoTrade.getSize());
    trade.setSide(gmoTrade.getSide());
    try {
      Instant timestamp =
          ZonedDateTime.parse(gmoTrade.getTimestamp(), DateTimeFormatter.ISO_DATE_TIME).toInstant();
      trade.setTimestamp(timestamp);
    } catch (Exception e) {
      logger.warn("Failed to parse GMO timestamp: {}", gmoTrade.getTimestamp(), e);
      trade.setTimestamp(Instant.now()); // Fallback
    }
    trade.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
    logger.debug("Converted GMO trade: {}", trade);
    return trade;
  }

  private MarketBoard convertToDomainMarketBoard(GmoOrderbook gmoOrderbook) {
    MarketBoard board = new MarketBoard();
    board.setExchange("GMO");
    board.setSymbol(gmoOrderbook.getSymbol());
    try {
      board.setTs(
          ZonedDateTime.parse(gmoOrderbook.getTimestamp(), DateTimeFormatter.ISO_DATE_TIME)
              .toInstant());
    } catch (Exception e) {
      logger.warn("Failed to parse GMO timestamp: {}", gmoOrderbook.getTimestamp(), e);
      board.setTs(Instant.now()); // Fallback
    }

    // Map top 8 bids/asks
    mapPriceLevels(gmoOrderbook.getBids(), board, true);
    mapPriceLevels(gmoOrderbook.getAsks(), board, false);

    return board;
  }

  private void mapPriceLevels(
      List<GmoOrderbook.PriceLevel> levels, MarketBoard board, boolean isBid) {
    if (levels == null) return;
    for (int i = 0; i < Math.min(levels.size(), 8); i++) {
      GmoOrderbook.PriceLevel level = levels.get(i);
      if (level == null || level.getPrice() == null || level.getSize() == null) continue;

      switch (i) {
        case 0:
          if (isBid) {
            board.setBid1(level.getPrice());
            board.setBid1vol(level.getSize());
          } else {
            board.setAsk1(level.getPrice());
            board.setAsk1vol(level.getSize());
          }
          break;
        case 1:
          if (isBid) {
            board.setBid2(level.getPrice());
            board.setBid2vol(level.getSize());
          } else {
            board.setAsk2(level.getPrice());
            board.setAsk2vol(level.getSize());
          }
          break;
        case 2:
          if (isBid) {
            board.setBid3(level.getPrice());
            board.setBid3vol(level.getSize());
          } else {
            board.setAsk3(level.getPrice());
            board.setAsk3vol(level.getSize());
          }
          break;
        case 3:
          if (isBid) {
            board.setBid4(level.getPrice());
            board.setBid4vol(level.getSize());
          } else {
            board.setAsk4(level.getPrice());
            board.setAsk4vol(level.getSize());
          }
          break;
        case 4:
          if (isBid) {
            board.setBid5(level.getPrice());
            board.setBid5vol(level.getSize());
          } else {
            board.setAsk5(level.getPrice());
            board.setAsk5vol(level.getSize());
          }
          break;
        case 5:
          if (isBid) {
            board.setBid6(level.getPrice());
            board.setBid6vol(level.getSize());
          } else {
            board.setAsk6(level.getPrice());
            board.setAsk6vol(level.getSize());
          }
          break;
        case 6:
          if (isBid) {
            board.setBid7(level.getPrice());
            board.setBid7vol(level.getSize());
          } else {
            board.setAsk7(level.getPrice());
            board.setAsk7vol(level.getSize());
          }
          break;
        case 7:
          if (isBid) {
            board.setBid8(level.getPrice());
            board.setBid8vol(level.getSize());
          } else {
            board.setAsk8(level.getPrice());
            board.setAsk8vol(level.getSize());
          }
          break;
      }
    }
  }
}
