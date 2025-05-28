package com.example.bitcoinmarketrecorder.bitflyer;

import com.example.bitcoinmarketrecorder.bitflyer.model.BitflyerBoard;
import com.example.bitcoinmarketrecorder.bitflyer.model.BitflyerExecution;
import com.example.bitcoinmarketrecorder.model.BestBidAsk;
import com.example.bitcoinmarketrecorder.model.MarketBoard;
import com.example.bitcoinmarketrecorder.model.Trade;
import com.example.bitcoinmarketrecorder.service.DataPersistenceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
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
public class BitflyerWebSocketClient {

  private static final Logger logger = LoggerFactory.getLogger(BitflyerWebSocketClient.class);

  @Value("${bitflyer.api.ws-url}")
  private String wsUrl;

  private final WebSocketClient client = new ReactorNettyWebSocketClient();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final DataPersistenceService persistenceService;
  private Disposable connectionDisposable;
  private final AtomicLong jsonRpcId = new AtomicLong(1);

  // Symbols defined in README.md (Bitflyer versions)
  private static final String SYMBOL_BTC_SPOT = "BTC_JPY";
  private static final String SYMBOL_BTC_FX = "FX_BTC_JPY";

  // Bitflyer channel names
  private static final String CHANNEL_BOARD_SNAPSHOT_PREFIX = "lightning_board_snapshot_";
  private static final String CHANNEL_BOARD_DELTA_PREFIX = "lightning_board_";
  private static final String CHANNEL_EXECUTIONS_PREFIX = "lightning_executions_";

  // Store the latest market board data for each symbol
  private final Map<String, MarketBoard> latestBoards = new ConcurrentHashMap<>();

  @Autowired
  public BitflyerWebSocketClient(DataPersistenceService persistenceService) {
    this.persistenceService = persistenceService;
  }

  @PostConstruct
  public void connect() {
    if (connectionDisposable == null || connectionDisposable.isDisposed()) {
      String wsUrl = "wss://ws.lightstream.bitflyer.com/json-rpc";
      logger.info("Connecting to Bitflyer WebSocket: {}", wsUrl);
      connectionDisposable =
          client
              .execute(
                  URI.create(wsUrl),
                  session -> {
                    // Create Flux of subscription messages with delays
                    Flux<WebSocketMessage> subscriptionMessages =
                        Flux.concat(
                            // Subscribe to board snapshots first
                            createSubscriptionMessageMono(
                                    session, CHANNEL_BOARD_SNAPSHOT_PREFIX + SYMBOL_BTC_SPOT)
                                .delayElement(Duration.ofSeconds(1)),
                            createSubscriptionMessageMono(
                                    session, CHANNEL_BOARD_SNAPSHOT_PREFIX + SYMBOL_BTC_FX)
                                .delayElement(Duration.ofSeconds(1)),
                            // Then subscribe to board deltas
                            createSubscriptionMessageMono(
                                    session, CHANNEL_BOARD_DELTA_PREFIX + SYMBOL_BTC_SPOT)
                                .delayElement(Duration.ofSeconds(1)),
                            createSubscriptionMessageMono(
                                    session, CHANNEL_BOARD_DELTA_PREFIX + SYMBOL_BTC_FX)
                                .delayElement(Duration.ofSeconds(1)),
                            // Finally subscribe to executions
                            createSubscriptionMessageMono(
                                    session, CHANNEL_EXECUTIONS_PREFIX + SYMBOL_BTC_SPOT)
                                .delayElement(Duration.ofSeconds(1)),
                            createSubscriptionMessageMono(
                                    session, CHANNEL_EXECUTIONS_PREFIX + SYMBOL_BTC_FX)
                                .delayElement(Duration.ofSeconds(1)));

                    // Send messages and then start receiving
                    return session
                        .send(subscriptionMessages)
                        .thenMany(
                            session
                                .receive()
                                .map(WebSocketMessage::getPayloadAsText)
                                .publishOn(Schedulers.boundedElastic())
                                .flatMap(this::handleMessage)
                                .onErrorResume(
                                    e -> {
                                      logger.error(
                                          "Error processing Bitflyer WebSocket message: {}",
                                          e.getMessage(),
                                          e);
                                      return Mono.empty();
                                    }))
                        .then();
                  })
              .retryWhen(
                  Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(5))
                      .maxBackoff(Duration.ofMinutes(1)))
              .doOnError(
                  error ->
                      logger.error("Bitflyer WebSocket connection error: {}", error.getMessage()))
              .doFinally(
                  signalType -> logger.warn("Bitflyer WebSocket connection closed: {}", signalType))
              .subscribe(
                  null,
                  error ->
                      logger.error(
                          "Bitflyer WebSocket connection failed permanently: {}",
                          error.getMessage()),
                  () -> logger.info("Bitflyer WebSocket connection sequence completed."));
    }
  }

  @PreDestroy
  public void disconnect() {
    if (connectionDisposable != null && !connectionDisposable.isDisposed()) {
      logger.info("Disconnecting from Bitflyer WebSocket.");
      connectionDisposable.dispose();
    }
  }

  private Mono<WebSocketMessage> createSubscriptionMessageMono(
      org.springframework.web.reactive.socket.WebSocketSession session, String channel) {
    try {
      Map<String, Object> messageMap =
          Map.of(
              "jsonrpc",
              "2.0",
              "method",
              "subscribe",
              "params",
              Map.of("channel", channel),
              "id",
              jsonRpcId.getAndIncrement());
      String jsonMessage = objectMapper.writeValueAsString(messageMap);
      return Mono.just(session.textMessage(jsonMessage));
    } catch (JsonProcessingException e) {
      logger.error(
          "Failed to create Bitflyer subscription message for {}: {}", channel, e.getMessage());
      return Mono.error(e);
    }
  }

  private Mono<Void> handleMessage(String jsonPayload) {
    try {
      JsonNode rootNode = objectMapper.readTree(jsonPayload);
      if (rootNode.has("method") && "channelMessage".equals(rootNode.get("method").asText())) {
        JsonNode params = rootNode.get("params");
        String channel = params.get("channel").asText();
        JsonNode message = params.get("message");

        if (channel.startsWith(CHANNEL_EXECUTIONS_PREFIX)) {
          handleExecutionsMessage(channel, message);
        } else if (channel.startsWith(CHANNEL_BOARD_SNAPSHOT_PREFIX)) {
          handleBoardSnapshotMessage(channel, message);
        } else if (channel.startsWith(CHANNEL_BOARD_DELTA_PREFIX)) {
          handleBoardDeltaMessage(channel, message);
        } else {
          logger.debug("Received unhandled channel message: {}", channel);
        }
      } else if (rootNode.has("result")) {
        logger.info("Received subscription confirmation: {}", rootNode.get("result"));
      } else if (rootNode.has("error")) {
        logger.error("Received error from Bitflyer: {}", rootNode.get("error"));
      } else {
        logger.debug("Received other JSON-RPC message: {}", jsonPayload);
      }
    } catch (JsonProcessingException e) {
      logger.warn("Failed to parse Bitflyer WebSocket message: {}", jsonPayload, e);
    } catch (Exception e) {
      logger.error("Error handling Bitflyer message: {}", jsonPayload, e);
    }
    return Mono.empty();
  }

  private void handleExecutionsMessage(String channel, JsonNode message)
      throws JsonProcessingException {
    String symbol = channel.substring(CHANNEL_EXECUTIONS_PREFIX.length());
    List<BitflyerExecution> executions =
        objectMapper.convertValue(message, new TypeReference<List<BitflyerExecution>>() {});
    List<Trade> trades =
        executions.stream().map(exec -> convertToDomainTrade(exec, symbol)).toList();
    persistenceService.saveTrades(trades);
  }

  private void handleBoardSnapshotMessage(String channel, JsonNode message)
      throws JsonProcessingException {
    String symbol = channel.substring(CHANNEL_BOARD_SNAPSHOT_PREFIX.length());
    BitflyerBoard board = objectMapper.convertValue(message, BitflyerBoard.class);
    MarketBoard marketBoard = convertToDomainMarketBoard(board, symbol);
    latestBoards.put(symbol, marketBoard);
    persistenceService.saveMarketBoard(marketBoard);
    updateBestBidAsk(marketBoard);
  }

  private void handleBoardDeltaMessage(String channel, JsonNode message)
      throws JsonProcessingException {
    String symbol = channel.substring(CHANNEL_BOARD_DELTA_PREFIX.length());
    BitflyerBoard deltaBoard = objectMapper.convertValue(message, BitflyerBoard.class);

    // Get the latest board or create a new one if not exists
    MarketBoard latestBoard =
        latestBoards.computeIfAbsent(
            symbol,
            k -> {
              MarketBoard newBoard = new MarketBoard();
              newBoard.setExchange("BITFLYER");
              newBoard.setSymbol(symbol);
              return newBoard;
            });

    // Update the board with delta
    updateBoardWithDelta(latestBoard, deltaBoard);
    latestBoard.setTs(Instant.now());

    persistenceService.saveMarketBoard(latestBoard);
    updateBestBidAsk(latestBoard);
  }

  private void updateBoardWithDelta(MarketBoard board, BitflyerBoard delta) {
    // Update bids
    for (BitflyerBoard.PriceLevel level : delta.getBids()) {
      if (level.getSize().compareTo(BigDecimal.ZERO) == 0) {
        // Remove the price level if size is 0
        board.getBids().removeIf(bid -> bid.getPrice().compareTo(level.getPrice()) == 0);
      } else {
        // Update or add the price level
        boolean updated = false;
        for (int i = 0; i < board.getBids().size(); i++) {
          if (board.getBids().get(i).getPrice().compareTo(level.getPrice()) == 0) {
            board.getBids().set(i, new MarketBoard.PriceLevel(level.getPrice(), level.getSize()));
            updated = true;
            break;
          }
        }
        if (!updated) {
          board.getBids().add(new MarketBoard.PriceLevel(level.getPrice(), level.getSize()));
        }
      }
    }

    // Update asks
    for (BitflyerBoard.PriceLevel level : delta.getAsks()) {
      if (level.getSize().compareTo(BigDecimal.ZERO) == 0) {
        // Remove the price level if size is 0
        board.getAsks().removeIf(ask -> ask.getPrice().compareTo(level.getPrice()) == 0);
      } else {
        // Update or add the price level
        boolean updated = false;
        for (int i = 0; i < board.getAsks().size(); i++) {
          if (board.getAsks().get(i).getPrice().compareTo(level.getPrice()) == 0) {
            board.getAsks().set(i, new MarketBoard.PriceLevel(level.getPrice(), level.getSize()));
            updated = true;
            break;
          }
        }
        if (!updated) {
          board.getAsks().add(new MarketBoard.PriceLevel(level.getPrice(), level.getSize()));
        }
      }
    }

    // Sort bids (descending) and asks (ascending)
    board.getBids().sort((a, b) -> b.getPrice().compareTo(a.getPrice()));
    board.getAsks().sort((a, b) -> a.getPrice().compareTo(b.getPrice()));
  }

  private void updateBestBidAsk(MarketBoard board) {
    if (!board.getBids().isEmpty() && !board.getAsks().isEmpty()) {
      BestBidAsk bestBidAsk = new BestBidAsk();
      bestBidAsk.setExchange("BITFLYER");
      bestBidAsk.setSymbol(board.getSymbol());
      bestBidAsk.setBestBid(board.getBids().get(0).getPrice());
      bestBidAsk.setBestBidVolume(board.getBids().get(0).getSize());
      bestBidAsk.setBestAsk(board.getAsks().get(0).getPrice());
      bestBidAsk.setBestAskVolume(board.getAsks().get(0).getSize());
      bestBidAsk.setTimestamp(Instant.now());
      persistenceService.saveBestBidAsk(bestBidAsk);
    }
  }

  private Trade convertToDomainTrade(BitflyerExecution exec, String bitflyerSymbol) {
    Trade trade = new Trade();
    trade.setExchange("BITFLYER");
    trade.setSymbol(bitflyerSymbol);
    trade.setTradeId("BITFLYER-" + exec.getId());
    trade.setPrice(exec.getPrice());
    trade.setSize(exec.getSize());
    trade.setSide(exec.getSide());
    try {
      Instant timestamp =
          ZonedDateTime.parse(exec.getExecDate(), DateTimeFormatter.ISO_DATE_TIME).toInstant();
      trade.setTimestamp(timestamp);
    } catch (Exception e) {
      logger.warn("Failed to parse Bitflyer timestamp: {}", exec.getExecDate(), e);
      trade.setTimestamp(Instant.now());
    }
    trade.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
    return trade;
  }

  private MarketBoard convertToDomainMarketBoard(BitflyerBoard board, String symbol) {
    MarketBoard marketBoard = new MarketBoard();
    marketBoard.setExchange("BITFLYER");
    marketBoard.setSymbol(symbol);
    marketBoard.setTs(Instant.now());

    // Convert bids
    for (BitflyerBoard.PriceLevel level : board.getBids()) {
      marketBoard.getBids().add(new MarketBoard.PriceLevel(level.getPrice(), level.getSize()));
    }

    // Convert asks
    for (BitflyerBoard.PriceLevel level : board.getAsks()) {
      marketBoard.getAsks().add(new MarketBoard.PriceLevel(level.getPrice(), level.getSize()));
    }

    return marketBoard;
  }
}
