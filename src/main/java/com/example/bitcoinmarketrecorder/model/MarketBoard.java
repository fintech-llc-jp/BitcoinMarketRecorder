package com.example.bitcoinmarketrecorder.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MarketBoard {
  private String symbol;
  private String exchange;
  private List<PriceLevel> bids;
  private List<PriceLevel> asks;
  private Instant ts; // Timestamp from WebSocket

  public MarketBoard() {
    this.bids = new ArrayList<>();
    this.asks = new ArrayList<>();
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public String getExchange() {
    return exchange;
  }

  public void setExchange(String exchange) {
    this.exchange = exchange;
  }

  public List<PriceLevel> getBids() {
    return bids;
  }

  public void setBids(List<PriceLevel> bids) {
    this.bids = bids;
  }

  public List<PriceLevel> getAsks() {
    return asks;
  }

  public void setAsks(List<PriceLevel> asks) {
    this.asks = asks;
  }

  public Instant getTs() {
    return ts;
  }

  public void setTs(Instant ts) {
    this.ts = ts;
  }

  @Override
  public String toString() {
    return "MarketBoard{"
        + "symbol='"
        + symbol
        + '\''
        + ", exchange='"
        + exchange
        + '\''
        + ", bids="
        + bids
        + ", asks="
        + asks
        + ", ts="
        + ts
        + '}';
  }

  public static class PriceLevel {
    private BigDecimal price;
    private BigDecimal size;

    public PriceLevel() {}

    public PriceLevel(BigDecimal price, BigDecimal size) {
      this.price = price;
      this.size = size;
    }

    public BigDecimal getPrice() {
      return price;
    }

    public void setPrice(BigDecimal price) {
      this.price = price;
    }

    public BigDecimal getSize() {
      return size;
    }

    public void setSize(BigDecimal size) {
      this.size = size;
    }

    @Override
    public String toString() {
      return "PriceLevel{" + "price=" + price + ", size=" + size + '}';
    }
  }
}
