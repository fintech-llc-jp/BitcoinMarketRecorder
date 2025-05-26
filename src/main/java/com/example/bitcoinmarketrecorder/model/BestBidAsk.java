package com.example.bitcoinmarketrecorder.model;

import java.math.BigDecimal;
import java.time.Instant;

public class BestBidAsk {
  private String exchange;
  private String symbol;
  private BigDecimal bestBid;
  private BigDecimal bestBidVolume;
  private BigDecimal bestAsk;
  private BigDecimal bestAskVolume;
  private Instant timestamp;

  // Getters and Setters
  public String getExchange() {
    return exchange;
  }

  public void setExchange(String exchange) {
    this.exchange = exchange;
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public BigDecimal getBestBid() {
    return bestBid;
  }

  public void setBestBid(BigDecimal bestBid) {
    this.bestBid = bestBid;
  }

  public BigDecimal getBestBidVolume() {
    return bestBidVolume;
  }

  public void setBestBidVolume(BigDecimal bestBidVolume) {
    this.bestBidVolume = bestBidVolume;
  }

  public BigDecimal getBestAsk() {
    return bestAsk;
  }

  public void setBestAsk(BigDecimal bestAsk) {
    this.bestAsk = bestAsk;
  }

  public BigDecimal getBestAskVolume() {
    return bestAskVolume;
  }

  public void setBestAskVolume(BigDecimal bestAskVolume) {
    this.bestAskVolume = bestAskVolume;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }
}
