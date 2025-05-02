package com.example.bitcoinmarketrecorder.bitflyer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BitflyerBoard {

    @JsonProperty("mid_price")
    private BigDecimal midPrice;
    @JsonProperty("bids")
    private List<PriceLevel> bids;
    @JsonProperty("asks")
    private List<PriceLevel> asks;

    // Getters and Setters
    public BigDecimal getMidPrice() { return midPrice; }
    public void setMidPrice(BigDecimal midPrice) { this.midPrice = midPrice; }
    public List<PriceLevel> getBids() { return bids; }
    public void setBids(List<PriceLevel> bids) { this.bids = bids; }
    public List<PriceLevel> getAsks() { return asks; }
    public void setAsks(List<PriceLevel> asks) { this.asks = asks; }

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
        return "BitflyerBoard{" +
               "midPrice=" + midPrice +
               ", bids=" + bids + // Consider limiting output size
               ", asks=" + asks + // Consider limiting output size
               '}';
    }
} 