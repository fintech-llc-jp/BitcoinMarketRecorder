package com.example.bitcoinmarketrecorder.model;

import java.math.BigDecimal;
import java.time.Instant;

public class MarketBoard {
    private String symbol;
    private String exchange;
    private BigDecimal bid1;
    private BigDecimal bid2;
    private BigDecimal bid3;
    private BigDecimal bid4;
    private BigDecimal bid5;
    private BigDecimal bid6;
    private BigDecimal bid7;
    private BigDecimal bid8;
    private BigDecimal ask1;
    private BigDecimal ask2;
    private BigDecimal ask3;
    private BigDecimal ask4;
    private BigDecimal ask5;
    private BigDecimal ask6;
    private BigDecimal ask7;
    private BigDecimal ask8;
    private BigDecimal bid1vol;
    private BigDecimal bid2vol;
    private BigDecimal bid3vol;
    private BigDecimal bid4vol;
    private BigDecimal bid5vol;
    private BigDecimal bid6vol;
    private BigDecimal bid7vol;
    private BigDecimal bid8vol;
    private BigDecimal ask1vol;
    private BigDecimal ask2vol;
    private BigDecimal ask3vol;
    private BigDecimal ask4vol;
    private BigDecimal ask5vol;
    private BigDecimal ask6vol;
    private BigDecimal ask7vol;
    private BigDecimal ask8vol;
    private Instant ts; // Timestamp from WebSocket

    // Getters and Setters

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

    public BigDecimal getBid1() {
        return bid1;
    }

    public void setBid1(BigDecimal bid1) {
        this.bid1 = bid1;
    }

    public BigDecimal getBid2() {
        return bid2;
    }

    public void setBid2(BigDecimal bid2) {
        this.bid2 = bid2;
    }

    public BigDecimal getBid3() {
        return bid3;
    }

    public void setBid3(BigDecimal bid3) {
        this.bid3 = bid3;
    }

    public BigDecimal getBid4() {
        return bid4;
    }

    public void setBid4(BigDecimal bid4) {
        this.bid4 = bid4;
    }

    public BigDecimal getBid5() {
        return bid5;
    }

    public void setBid5(BigDecimal bid5) {
        this.bid5 = bid5;
    }

    public BigDecimal getBid6() {
        return bid6;
    }

    public void setBid6(BigDecimal bid6) {
        this.bid6 = bid6;
    }

    public BigDecimal getBid7() {
        return bid7;
    }

    public void setBid7(BigDecimal bid7) {
        this.bid7 = bid7;
    }

    public BigDecimal getBid8() {
        return bid8;
    }

    public void setBid8(BigDecimal bid8) {
        this.bid8 = bid8;
    }

    public BigDecimal getAsk1() {
        return ask1;
    }

    public void setAsk1(BigDecimal ask1) {
        this.ask1 = ask1;
    }

    public BigDecimal getAsk2() {
        return ask2;
    }

    public void setAsk2(BigDecimal ask2) {
        this.ask2 = ask2;
    }

    public BigDecimal getAsk3() {
        return ask3;
    }

    public void setAsk3(BigDecimal ask3) {
        this.ask3 = ask3;
    }

    public BigDecimal getAsk4() {
        return ask4;
    }

    public void setAsk4(BigDecimal ask4) {
        this.ask4 = ask4;
    }

    public BigDecimal getAsk5() {
        return ask5;
    }

    public void setAsk5(BigDecimal ask5) {
        this.ask5 = ask5;
    }

    public BigDecimal getAsk6() {
        return ask6;
    }

    public void setAsk6(BigDecimal ask6) {
        this.ask6 = ask6;
    }

    public BigDecimal getAsk7() {
        return ask7;
    }

    public void setAsk7(BigDecimal ask7) {
        this.ask7 = ask7;
    }

    public BigDecimal getAsk8() {
        return ask8;
    }

    public void setAsk8(BigDecimal ask8) {
        this.ask8 = ask8;
    }

    public BigDecimal getBid1vol() {
        return bid1vol;
    }

    public void setBid1vol(BigDecimal bid1vol) {
        this.bid1vol = bid1vol;
    }

    public BigDecimal getBid2vol() {
        return bid2vol;
    }

    public void setBid2vol(BigDecimal bid2vol) {
        this.bid2vol = bid2vol;
    }

    public BigDecimal getBid3vol() {
        return bid3vol;
    }

    public void setBid3vol(BigDecimal bid3vol) {
        this.bid3vol = bid3vol;
    }

    public BigDecimal getBid4vol() {
        return bid4vol;
    }

    public void setBid4vol(BigDecimal bid4vol) {
        this.bid4vol = bid4vol;
    }

    public BigDecimal getBid5vol() {
        return bid5vol;
    }

    public void setBid5vol(BigDecimal bid5vol) {
        this.bid5vol = bid5vol;
    }

    public BigDecimal getBid6vol() {
        return bid6vol;
    }

    public void setBid6vol(BigDecimal bid6vol) {
        this.bid6vol = bid6vol;
    }

    public BigDecimal getBid7vol() {
        return bid7vol;
    }

    public void setBid7vol(BigDecimal bid7vol) {
        this.bid7vol = bid7vol;
    }

    public BigDecimal getBid8vol() {
        return bid8vol;
    }

    public void setBid8vol(BigDecimal bid8vol) {
        this.bid8vol = bid8vol;
    }

    public BigDecimal getAsk1vol() {
        return ask1vol;
    }

    public void setAsk1vol(BigDecimal ask1vol) {
        this.ask1vol = ask1vol;
    }

    public BigDecimal getAsk2vol() {
        return ask2vol;
    }

    public void setAsk2vol(BigDecimal ask2vol) {
        this.ask2vol = ask2vol;
    }

    public BigDecimal getAsk3vol() {
        return ask3vol;
    }

    public void setAsk3vol(BigDecimal ask3vol) {
        this.ask3vol = ask3vol;
    }

    public BigDecimal getAsk4vol() {
        return ask4vol;
    }

    public void setAsk4vol(BigDecimal ask4vol) {
        this.ask4vol = ask4vol;
    }

    public BigDecimal getAsk5vol() {
        return ask5vol;
    }

    public void setAsk5vol(BigDecimal ask5vol) {
        this.ask5vol = ask5vol;
    }

    public BigDecimal getAsk6vol() {
        return ask6vol;
    }

    public void setAsk6vol(BigDecimal ask6vol) {
        this.ask6vol = ask6vol;
    }

    public BigDecimal getAsk7vol() {
        return ask7vol;
    }

    public void setAsk7vol(BigDecimal ask7vol) {
        this.ask7vol = ask7vol;
    }

    public BigDecimal getAsk8vol() {
        return ask8vol;
    }

    public void setAsk8vol(BigDecimal ask8vol) {
        this.ask8vol = ask8vol;
    }

    public Instant getTs() {
        return ts;
    }

    public void setTs(Instant ts) {
        this.ts = ts;
    }

    @Override
    public String toString() {
        return "MarketBoard{" +
               "symbol='" + symbol + '\'' +
               ", exchange='" + exchange + '\'' +
               ", ts=" + ts +
                // ... (omitting all price/volume fields for brevity)
               '}';
    }
} 