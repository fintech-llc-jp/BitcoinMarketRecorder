package com.example.bitcoinmarketrecorder.service;

import com.example.bitcoinmarketrecorder.config.ExchSimProperties;
import com.example.bitcoinmarketrecorder.model.MarketBoard;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExchSimService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExchSimService.class);
    
    @Autowired
    private ExchSimProperties exchSimProperties;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> tokenCache = new ConcurrentHashMap<>();
    
    public void processTradeData(com.example.bitcoinmarketrecorder.model.Trade trade) {
        if (!exchSimProperties.isEnabled()) {
            logger.debug("ExchSim integration is disabled");
            return;
        }
        
        String targetSymbol = exchSimProperties.mapSymbol(
            trade.getExchange().toUpperCase(), 
            trade.getSymbol()
        );
        
        if (targetSymbol == null) {
            logger.debug("No symbol mapping found for {}:{}", 
                trade.getExchange(), trade.getSymbol());
            return;
        }
        
        try {
            String token = getAuthToken();
            if (token != null) {
                sendTradeInsertRequest(token, targetSymbol, trade);
            }
        } catch (Exception e) {
            logger.error("Failed to process trade data for symbol {}: {}", 
                targetSymbol, e.getMessage(), e);
        }
    }
    
    public void processMarketBoard(MarketBoard marketBoard) {
        if (!exchSimProperties.isEnabled()) {
            logger.debug("ExchSim integration is disabled");
            return;
        }
        
        String targetSymbol = exchSimProperties.mapSymbol(
            marketBoard.getExchange().toUpperCase(), 
            marketBoard.getSymbol()
        );
        
        if (targetSymbol == null) {
            logger.debug("No symbol mapping found for {}:{}", 
                marketBoard.getExchange(), marketBoard.getSymbol());
            return;
        }
        
        try {
            String token = getAuthToken();
            if (token != null) {
                sendMarketMakeOrder(token, targetSymbol, marketBoard);
            }
        } catch (Exception e) {
            logger.error("Failed to process market board for symbol {}: {}", 
                targetSymbol, e.getMessage(), e);
        }
    }
    
    private String getAuthToken() {
        String cacheKey = exchSimProperties.getApi().getUsername();
        String cachedToken = tokenCache.get(cacheKey);
        
        if (cachedToken != null) {
            return cachedToken;
        }
        
        try {
            String loginUrl = exchSimProperties.getApi().getBaseUrl() + "/api/auth/login";
            
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", exchSimProperties.getApi().getUsername());
            loginRequest.put("password", exchSimProperties.getApi().getPassword());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(loginRequest, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(loginUrl, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String token = (String) response.getBody().get("token");
                if (token != null) {
                    tokenCache.put(cacheKey, token);
                    logger.info("Successfully authenticated with exch_sim");
                    return token;
                }
            }
            
            logger.error("Failed to authenticate with exch_sim: {}", response.getStatusCode());
            return null;
            
        } catch (Exception e) {
            logger.error("Error during exch_sim authentication: {}", e.getMessage(), e);
            return null;
        }
    }
    
    private void sendTradeInsertRequest(String token, String symbol, com.example.bitcoinmarketrecorder.model.Trade trade) {
        try {
            String tradeInsertUrl = exchSimProperties.getApi().getBaseUrl() + "/api/trade/insert";
            
            TradeInsertRequest request = convertToTradeInsertRequest(symbol, trade);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);
            
            HttpEntity<TradeInsertRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(tradeInsertUrl, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully sent trade insert request for symbol: {}, price: {}, size: {}, side: {}", 
                    symbol, trade.getPrice(), trade.getSize(), trade.getSide());
                logger.debug("Trade insert response: {}", response.getBody());
            } else {
                logger.error("Failed to send trade insert request for symbol {}: {}", 
                    symbol, response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Error sending trade insert request for symbol {}: {}", 
                symbol, e.getMessage(), e);
            
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                tokenCache.clear();
                logger.info("Cleared auth token cache due to 401 error");
            }
        }
    }
    
    private void sendMarketMakeOrder(String token, String symbol, MarketBoard marketBoard) {
        try {
            String marketMakeUrl = exchSimProperties.getApi().getBaseUrl() + "/api/market-make/orders";
            
            MarketMakeRequest request = convertToMarketMakeRequest(symbol, marketBoard);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);
            
            HttpEntity<MarketMakeRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(marketMakeUrl, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully sent market make order for symbol: {}", symbol);
                logger.debug("Market make response: {}", response.getBody());
            } else {
                logger.error("Failed to send market make order for symbol {}: {}", 
                    symbol, response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Error sending market make order for symbol {}: {}", 
                symbol, e.getMessage(), e);
            
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                tokenCache.clear();
                logger.info("Cleared auth token cache due to 401 error");
            }
        }
    }
    
    private TradeInsertRequest convertToTradeInsertRequest(String symbol, com.example.bitcoinmarketrecorder.model.Trade trade) {
        TradeInsertRequest request = new TradeInsertRequest();
        request.setSymbol(symbol);
        request.setPrice(trade.getPrice().doubleValue());
        request.setQuantity(trade.getSize().doubleValue());
        request.setSide(trade.getSide());
        
        return request;
    }
    
    private MarketMakeRequest convertToMarketMakeRequest(String symbol, MarketBoard marketBoard) {
        MarketMakeRequest request = new MarketMakeRequest();
        request.setSymbol(symbol);
        
        List<PriceLevel> bidLevels = new ArrayList<>();
        List<PriceLevel> askLevels = new ArrayList<>();
        
        int maxLevels = Math.min(5, Math.max(
            marketBoard.getBids().size(), 
            marketBoard.getAsks().size()
        ));
        
        for (int i = 0; i < Math.min(maxLevels, marketBoard.getBids().size()); i++) {
            MarketBoard.PriceLevel bid = marketBoard.getBids().get(i);
            bidLevels.add(new PriceLevel(bid.getPrice().doubleValue(), bid.getSize().doubleValue()));
        }
        
        for (int i = 0; i < Math.min(maxLevels, marketBoard.getAsks().size()); i++) {
            MarketBoard.PriceLevel ask = marketBoard.getAsks().get(i);
            askLevels.add(new PriceLevel(ask.getPrice().doubleValue(), ask.getSize().doubleValue()));
        }
        
        request.setBidLevels(bidLevels);
        request.setAskLevels(askLevels);
        
        return request;
    }
    
    public static class MarketMakeRequest {
        private String symbol;
        private List<PriceLevel> bidLevels;
        private List<PriceLevel> askLevels;
        
        public String getSymbol() {
            return symbol;
        }
        
        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }
        
        public List<PriceLevel> getBidLevels() {
            return bidLevels;
        }
        
        public void setBidLevels(List<PriceLevel> bidLevels) {
            this.bidLevels = bidLevels;
        }
        
        public List<PriceLevel> getAskLevels() {
            return askLevels;
        }
        
        public void setAskLevels(List<PriceLevel> askLevels) {
            this.askLevels = askLevels;
        }
    }
    
    public static class TradeInsertRequest {
        private String symbol;
        private double price;
        private double quantity;
        private String side;
        
        public String getSymbol() {
            return symbol;
        }
        
        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }
        
        public double getPrice() {
            return price;
        }
        
        public void setPrice(double price) {
            this.price = price;
        }
        
        public double getQuantity() {
            return quantity;
        }
        
        public void setQuantity(double quantity) {
            this.quantity = quantity;
        }
        
        public String getSide() {
            return side;
        }
        
        public void setSide(String side) {
            this.side = side;
        }
    }
    
    public static class PriceLevel {
        private double price;
        private double quantity;
        
        public PriceLevel() {}
        
        public PriceLevel(double price, double quantity) {
            this.price = price;
            this.quantity = quantity;
        }
        
        public double getPrice() {
            return price;
        }
        
        public void setPrice(double price) {
            this.price = price;
        }
        
        public double getQuantity() {
            return quantity;
        }
        
        public void setQuantity(double quantity) {
            this.quantity = quantity;
        }
    }
}