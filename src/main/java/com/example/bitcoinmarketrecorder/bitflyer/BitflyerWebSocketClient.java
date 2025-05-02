package com.example.bitcoinmarketrecorder.bitflyer;

import com.example.bitcoinmarketrecorder.bitflyer.model.BitflyerBoard;
import com.example.bitcoinmarketrecorder.bitflyer.model.BitflyerExecution;
import com.example.bitcoinmarketrecorder.model.Trade;
import com.example.bitcoinmarketrecorder.service.DataPersistenceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.concurrent.atomic.AtomicLong;

@Component
public class BitflyerWebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(BitflyerWebSocketClient.class);

    @Value("${bitflyer.api.ws-url}")
    private String wsUrl;

    private final WebSocketClient client = new ReactorNettyWebSocketClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DataPersistenceService persistenceService;
    private Disposable connectionDisposable;
    private final AtomicLong jsonRpcId = new AtomicLong(1); // For unique JSON-RPC request IDs

    // Symbols defined in README.md (Bitflyer versions)
    private static final String SYMBOL_BTC_SPOT = "BTC_JPY"; // 現物
    private static final String SYMBOL_BTC_FX = "FX_BTC_JPY"; // CFD (FX)

    // Bitflyer channel names
    private static final String CHANNEL_BOARD_PREFIX = "lightning_board_";
    private static final String CHANNEL_EXECUTIONS_PREFIX = "lightning_executions_";

    @Autowired
    public BitflyerWebSocketClient(DataPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @PostConstruct
    public void connect() {
        if (connectionDisposable == null || connectionDisposable.isDisposed()) {
            logger.info("Connecting to Bitflyer WebSocket: {}", wsUrl);
            connectionDisposable = client.execute(
                URI.create(wsUrl),
                session -> {
                    // Create Flux of subscription messages with delays
                    Flux<WebSocketMessage> subscriptionMessages = Flux.concat(
                        createSubscriptionMessageMono(session, CHANNEL_BOARD_PREFIX + SYMBOL_BTC_SPOT).delayElement(Duration.ofSeconds(1)),
                        createSubscriptionMessageMono(session, CHANNEL_EXECUTIONS_PREFIX + SYMBOL_BTC_SPOT).delayElement(Duration.ofSeconds(1)),
                        createSubscriptionMessageMono(session, CHANNEL_BOARD_PREFIX + SYMBOL_BTC_FX).delayElement(Duration.ofSeconds(1)),
                        createSubscriptionMessageMono(session, CHANNEL_EXECUTIONS_PREFIX + SYMBOL_BTC_FX).delayElement(Duration.ofSeconds(1))
                    );

                    // Send messages and then start receiving
                    return session.send(subscriptionMessages)
                           .thenMany(session.receive()
                                .map(WebSocketMessage::getPayloadAsText)
                                .publishOn(Schedulers.boundedElastic()) // Process on separate thread
                                .flatMap(this::handleMessage)
                                .onErrorResume(e -> {
                                    logger.error("Error processing Bitflyer WebSocket message: {}", e.getMessage(), e);
                                    return Mono.empty(); // Continue processing
                                })
                           )
                           .then(); // Complete Mono<Void> when receiving completes
                }
            )
            .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(5)).maxBackoff(Duration.ofMinutes(1)))
            .doOnError(error -> logger.error("Bitflyer WebSocket connection error: {}", error.getMessage()))
            .doFinally(signalType -> logger.warn("Bitflyer WebSocket connection closed: {}", signalType))
            .subscribe(
                null, // onNext not needed
                error -> logger.error("Bitflyer WebSocket connection failed permanently: {}", error.getMessage()),
                () -> logger.info("Bitflyer WebSocket connection sequence completed.")
            );
        }
    }

    @PreDestroy
    public void disconnect() {
        if (connectionDisposable != null && !connectionDisposable.isDisposed()) {
            logger.info("Disconnecting from Bitflyer WebSocket.");
            connectionDisposable.dispose();
        }
    }

    private Mono<WebSocketMessage> createSubscriptionMessageMono(org.springframework.web.reactive.socket.WebSocketSession session, String channel) {
        try {
            Map<String, Object> messageMap = Map.of(
                "jsonrpc", "2.0",
                "method", "subscribe",
                "params", Map.of("channel", channel),
                "id", jsonRpcId.getAndIncrement()
            );
            String jsonMessage = objectMapper.writeValueAsString(messageMap);
            // Return a Mono containing the message to be sent
            return Mono.just(session.textMessage(jsonMessage)); 
        } catch (JsonProcessingException e) {
            logger.error("Failed to create Bitflyer subscription message for {}: {}", channel, e.getMessage());
            return Mono.error(e);
        }
    }

    private Mono<Void> handleMessage(String jsonPayload) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonPayload);
            // Check if it's a notification message (has "method" and "params")
            if (rootNode.has("method") && "channelMessage".equals(rootNode.get("method").asText())) {
                JsonNode params = rootNode.get("params");
                String channel = params.get("channel").asText();
                JsonNode message = params.get("message");

                if (channel.startsWith(CHANNEL_EXECUTIONS_PREFIX)) {
                     String symbol = channel.substring(CHANNEL_EXECUTIONS_PREFIX.length());
                     // Execution messages are arrays
                     List<BitflyerExecution> executions = objectMapper.convertValue(message, new TypeReference<List<BitflyerExecution>>() {});
                     List<Trade> trades = executions.stream()
                         .map(exec -> convertToDomainTrade(exec, symbol))
                         .toList();
                     persistenceService.saveTrades(trades); 
                } else if (channel.startsWith(CHANNEL_BOARD_PREFIX)) {
                    String symbol = channel.substring(CHANNEL_BOARD_PREFIX.length());
                    BitflyerBoard board = objectMapper.convertValue(message, BitflyerBoard.class);
                    // TODO: Convert BitflyerBoard to MarketBoard and potentially save
                    // MarketBoard marketBoard = convertToDomainMarketBoard(board, symbol);
                    // logger.debug("Received Bitflyer MarketBoard for {}: {}", symbol, marketBoard);
                    // persistenceService.saveMarketBoard(marketBoard);
                } else {
                     // logger.debug("Received unhandled channel message: {}", channel);
                }
            } else if (rootNode.has("result")) {
                logger.info("Received subscription confirmation: {}", rootNode.get("result"));
            } else if (rootNode.has("error")) {
                logger.error("Received error from Bitflyer: {}", rootNode.get("error"));
            } else {
                // logger.debug("Received other JSON-RPC message: {}", jsonPayload);
            }
        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse Bitflyer WebSocket message: {}", jsonPayload, e);
        } catch (Exception e) {
             logger.error("Error handling Bitflyer message: {}", jsonPayload, e);
        }
        return Mono.empty();
    }

    private Trade convertToDomainTrade(BitflyerExecution exec, String bitflyerSymbol) {
        Trade trade = new Trade();
        trade.setExchange("BITFLYER");
        // Bitflyer symbols already match the desired format
        trade.setSymbol(bitflyerSymbol); 
        trade.setTradeId("BITFLYER-" + exec.getId()); // Ensure unique ID
        trade.setPrice(exec.getPrice());
        trade.setSize(exec.getSize());
        trade.setSide(exec.getSide());
        try {
            // Parse Bitflyer timestamp string (ISO 8601 format with Z)
            // Example: 2015-07-08T02:51:38.123Z - Use ZonedDateTime or OffsetDateTime
            Instant timestamp = ZonedDateTime.parse(exec.getExecDate(), DateTimeFormatter.ISO_DATE_TIME).toInstant();
            trade.setTimestamp(timestamp);
        } catch (Exception e) {
            logger.warn("Failed to parse Bitflyer timestamp: {}", exec.getExecDate(), e);
            trade.setTimestamp(Instant.now()); // Fallback
        }
        trade.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC)); // Record creation time
        return trade;
    }

    // TODO: Implement conversion logic if MarketBoard saving is needed
    /*
    private MarketBoard convertToDomainMarketBoard(BitflyerBoard bitflyerBoard, String bitflyerSymbol) {
        MarketBoard board = new MarketBoard();
        board.setExchange("BITFLYER");
        board.setSymbol(bitflyerSymbol);
        board.setTs(Instant.now()); // Bitflyer board snapshot doesn't have a specific timestamp in the message itself
        
        // Map top 8 bids/asks
        mapPriceLevels(bitflyerBoard.getBids(), board, true);
        mapPriceLevels(bitflyerBoard.getAsks(), board, false);
        
        return board;
    }
    
    private void mapPriceLevels(List<BitflyerBoard.PriceLevel> levels, MarketBoard board, boolean isBid) {
        if (levels == null) return;
         for (int i = 0; i < Math.min(levels.size(), 8); i++) {
            BitflyerBoard.PriceLevel level = levels.get(i);
             if (level == null || level.getPrice() == null || level.getSize() == null) continue;

            switch (i) {
                case 0: if (isBid) { board.setBid1(level.getPrice()); board.setBid1vol(level.getSize()); } else { board.setAsk1(level.getPrice()); board.setAsk1vol(level.getSize()); } break;
                case 1: if (isBid) { board.setBid2(level.getPrice()); board.setBid2vol(level.getSize()); } else { board.setAsk2(level.getPrice()); board.setAsk2vol(level.getSize()); } break;
                // ... cases 2-7
                case 7: if (isBid) { board.setBid8(level.getPrice()); board.setBid8vol(level.getSize()); } else { board.setAsk8(level.getPrice()); board.setAsk8vol(level.getSize()); } break;                
            }
        }
    }
    */
} 