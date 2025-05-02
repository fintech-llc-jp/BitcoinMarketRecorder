package com.example.bitcoinmarketrecorder.gmo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GmoTradeRecord {
    @JsonProperty("price")
    private BigDecimal price;
    @JsonProperty("side")
    private String side; // "BUY" or "SELL"
    @JsonProperty("size")
    private BigDecimal size;
    @JsonProperty("timestamp")
    private String timestamp; // e.g., "2018-03-16T02:51:38.123Z"
    @JsonProperty("symbol")
    private String symbol; // e.g., "BTC_JPY"
    @JsonProperty("executionId") // Assuming this field exists for trade ID
    private String executionId;

    // Getters and Setters
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public BigDecimal getSize() { return size; }
    public void setSize(BigDecimal size) { this.size = size; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    @Override
    public String toString() {
        return "GmoTradeRecord{" +
               "price=" + price +
               ", side='" + side + '\'' +
               ", size=" + size +
               ", timestamp='" + timestamp + '\'' +
               ", symbol='" + symbol + '\'' +
               ", executionId='" + executionId + '\'' +
               '}';
    }
} 