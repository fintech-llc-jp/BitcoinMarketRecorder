package com.example.bitcoinmarketrecorder.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public class Trade {
    private String exchange;           // 取引所（'GMO' or 'BITFLYER'）
    private String symbol;             // 取引ペア（'BTC_JPY', 'FX_BTC_JPY'など）
    private String tradeId;            // 約定ID
    private BigDecimal price;          // 約定価格
    private BigDecimal size;           // 約定数量
    private String side;               // 取引タイプ（'BUY' or 'SELL'）
    private Instant timestamp;         // 約定時刻 (Websocketから受け取るタイムスタンプ)
    private LocalDateTime createdAt;     // レコード作成時刻 (DB登録時のローカルタイム)

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

    public String getTradeId() {
        return tradeId;
    }

    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
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

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Trade{" +
               "exchange='" + exchange + '\'' +
               ", symbol='" + symbol + '\'' +
               ", tradeId='" + tradeId + '\'' +
               ", price=" + price +
               ", size=" + size +
               ", side='" + side + '\'' +
               ", timestamp=" + timestamp +
               ", createdAt=" + createdAt +
               '}';
    }
} 