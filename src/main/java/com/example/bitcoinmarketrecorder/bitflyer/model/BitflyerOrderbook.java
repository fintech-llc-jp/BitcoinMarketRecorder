package com.example.bitcoinmarketrecorder.bitflyer.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BitflyerOrderbook {
  private String symbol;
  private List<PriceLevel> bids = new ArrayList<>();
  private List<PriceLevel> asks = new ArrayList<>();

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
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

  public static class PriceLevel {
    private BigDecimal price;
    private BigDecimal size;

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
  }
}
