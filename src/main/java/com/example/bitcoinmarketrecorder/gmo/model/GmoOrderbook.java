package com.example.bitcoinmarketrecorder.gmo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GmoOrderbook {

    @JsonProperty("symbol")
    private String symbol;
    @JsonProperty("asks")
    private List<PriceLevel> asks;
    @JsonProperty("bids")
    private List<PriceLevel> bids;
    @JsonProperty("timestamp")
    private String timestamp; // e.g., "2018-03-16T02:51:38.123Z"

    // Getters and Setters
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public List<PriceLevel> getAsks() { return asks; }
    public void setAsks(List<PriceLevel> asks) { this.asks = asks; }
    public List<PriceLevel> getBids() { return bids; }
    public void setBids(List<PriceLevel> bids) { this.bids = bids; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceLevel {
        @JsonProperty("price")
        private BigDecimal price;
        @JsonProperty("size")
        private BigDecimal size;

        // Getters and Setters
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public BigDecimal getSize() { return size; }
        public void setSize(BigDecimal size) { this.size = size; }
    }

    @Override
    public String toString() {
        return "GmoOrderbook{" +
               "symbol='" + symbol + '\'' +
               ", asks=" + asks + // Consider limiting output size
               ", bids=" + bids + // Consider limiting output size
               ", timestamp='" + timestamp + '\'' +
               '}';
    }
} 