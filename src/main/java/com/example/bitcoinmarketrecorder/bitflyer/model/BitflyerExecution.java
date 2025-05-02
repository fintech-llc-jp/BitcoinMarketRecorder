package com.example.bitcoinmarketrecorder.bitflyer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BitflyerExecution {
    @JsonProperty("id")
    private long id;
    @JsonProperty("side")
    private String side;
    @JsonProperty("price")
    private BigDecimal price;
    @JsonProperty("size")
    private BigDecimal size;
    @JsonProperty("exec_date") // e.g., "2015-07-08T02:51:38.123Z"
    private String execDate;
    @JsonProperty("buy_child_order_acceptance_id")
    private String buyChildOrderAcceptanceId;
    @JsonProperty("sell_child_order_acceptance_id")
    private String sellChildOrderAcceptanceId;

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getSize() { return size; }
    public void setSize(BigDecimal size) { this.size = size; }
    public String getExecDate() { return execDate; }
    public void setExecDate(String execDate) { this.execDate = execDate; }
    public String getBuyChildOrderAcceptanceId() { return buyChildOrderAcceptanceId; }
    public void setBuyChildOrderAcceptanceId(String buyChildOrderAcceptanceId) { this.buyChildOrderAcceptanceId = buyChildOrderAcceptanceId; }
    public String getSellChildOrderAcceptanceId() { return sellChildOrderAcceptanceId; }
    public void setSellChildOrderAcceptanceId(String sellChildOrderAcceptanceId) { this.sellChildOrderAcceptanceId = sellChildOrderAcceptanceId; }

    @Override
    public String toString() {
        return "BitflyerExecution{" +
               "id=" + id +
               ", side='" + side + '\'' +
               ", price=" + price +
               ", size=" + size +
               ", execDate='" + execDate + '\'' +
               // ... other fields omitted for brevity
               '}';
    }
} 