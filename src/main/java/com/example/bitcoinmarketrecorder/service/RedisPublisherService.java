package com.example.bitcoinmarketrecorder.service;

import com.example.bitcoinmarketrecorder.config.RedisPublisherProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@org.springframework.context.annotation.DependsOn("redisConnectionFactory")
public class RedisPublisherService {

    private static final Logger logger = LoggerFactory.getLogger(RedisPublisherService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisPublisherProperties redisPublisherProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void publishTradeInsert(String symbol, ExchSimService.TradeInsertRequest request) {
        if (!redisPublisherProperties.isEnabled()) {
            logger.debug("Redis publishing is disabled");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String channel = redisPublisherProperties.getTradeInsert().getChannelName(symbol);
                String jsonMessage = objectMapper.writeValueAsString(request);
                
                redisTemplate.convertAndSend(channel, jsonMessage);
                
                logger.info("Successfully published trade insert to Redis channel: {}, symbol: {}, price: {}, quantity: {}, side: {}", 
                    channel, request.getSymbol(), request.getPrice(), request.getQuantity(), request.getSide());
                logger.debug("Trade insert message: {}", jsonMessage);
                
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize trade insert request for symbol {}: {}", 
                    symbol, e.getMessage(), e);
            } catch (Exception e) {
                logger.error("Failed to publish trade insert to Redis for symbol {}: {}", 
                    symbol, e.getMessage(), e);
            }
        });
    }

    public void publishMarketMake(String symbol, ExchSimService.MarketMakeRequest request) {
        if (!redisPublisherProperties.isEnabled()) {
            logger.debug("Redis publishing is disabled");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String channel = redisPublisherProperties.getMarketMake().getChannelName(symbol);
                String jsonMessage = objectMapper.writeValueAsString(request);
                
                redisTemplate.convertAndSend(channel, jsonMessage);
                
                logger.info("Successfully published market make to Redis channel: {}, symbol: {}, {} bidLevels, {} askLevels", 
                    channel, request.getSymbol(), 
                    request.getBidLevels() != null ? request.getBidLevels().size() : 0,
                    request.getAskLevels() != null ? request.getAskLevels().size() : 0);
                logger.debug("Market make message: {}", jsonMessage);
                
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize market make request for symbol {}: {}", 
                    symbol, e.getMessage(), e);
            } catch (Exception e) {
                logger.error("Failed to publish market make to Redis for symbol {}: {}", 
                    symbol, e.getMessage(), e);
            }
        });
    }

    public void publishTradeInsertSync(String symbol, ExchSimService.TradeInsertRequest request) {
        if (!redisPublisherProperties.isEnabled()) {
            logger.debug("Redis publishing is disabled");
            return;
        }

        try {
            String channel = redisPublisherProperties.getTradeInsert().getChannelName(symbol);
            String jsonMessage = objectMapper.writeValueAsString(request);
            
            redisTemplate.convertAndSend(channel, jsonMessage);
            
            logger.info("Successfully published trade insert to Redis channel: {}, symbol: {}, price: {}, quantity: {}, side: {}", 
                channel, request.getSymbol(), request.getPrice(), request.getQuantity(), request.getSide());
            logger.debug("Trade insert message: {}", jsonMessage);
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize trade insert request for symbol {}: {}", 
                symbol, e.getMessage(), e);
            throw new RuntimeException("Failed to serialize trade insert request", e);
        } catch (Exception e) {
            logger.error("Failed to publish trade insert to Redis for symbol {}: {}", 
                symbol, e.getMessage(), e);
            throw new RuntimeException("Failed to publish trade insert to Redis", e);
        }
    }

    public void publishMarketMakeSync(String symbol, ExchSimService.MarketMakeRequest request) {
        if (!redisPublisherProperties.isEnabled()) {
            logger.debug("Redis publishing is disabled");
            return;
        }

        try {
            String channel = redisPublisherProperties.getMarketMake().getChannelName(symbol);
            String jsonMessage = objectMapper.writeValueAsString(request);
            
            redisTemplate.convertAndSend(channel, jsonMessage);
            
            logger.info("Successfully published market make to Redis channel: {}, symbol: {}, {} bidLevels, {} askLevels", 
                channel, request.getSymbol(), 
                request.getBidLevels() != null ? request.getBidLevels().size() : 0,
                request.getAskLevels() != null ? request.getAskLevels().size() : 0);
            logger.debug("Market make message: {}", jsonMessage);
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize market make request for symbol {}: {}", 
                symbol, e.getMessage(), e);
            throw new RuntimeException("Failed to serialize market make request", e);
        } catch (Exception e) {
            logger.error("Failed to publish market make to Redis for symbol {}: {}", 
                symbol, e.getMessage(), e);
            throw new RuntimeException("Failed to publish market make to Redis", e);
        }
    }

    public boolean isHealthy() {
        try {
            redisTemplate.hasKey("health-check");
            return true;
        } catch (Exception e) {
            logger.warn("Redis health check failed: {}", e.getMessage());
            return false;
        }
    }
}