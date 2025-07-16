package com.example.bitcoinmarketrecorder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "redis.publisher")
public class RedisPublisherProperties {

    private boolean enabled = true;
    private MarketMake marketMake = new MarketMake();
    private TradeInsert tradeInsert = new TradeInsert();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public MarketMake getMarketMake() {
        return marketMake;
    }

    public void setMarketMake(MarketMake marketMake) {
        this.marketMake = marketMake;
    }

    public TradeInsert getTradeInsert() {
        return tradeInsert;
    }

    public void setTradeInsert(TradeInsert tradeInsert) {
        this.tradeInsert = tradeInsert;
    }

    public static class MarketMake {
        private String channelPrefix = "market-make";

        public String getChannelPrefix() {
            return channelPrefix;
        }

        public void setChannelPrefix(String channelPrefix) {
            this.channelPrefix = channelPrefix;
        }

        public String getChannelName(String symbol) {
            return channelPrefix + ":" + symbol;
        }
    }

    public static class TradeInsert {
        private String channelPrefix = "trade-insert";

        public String getChannelPrefix() {
            return channelPrefix;
        }

        public void setChannelPrefix(String channelPrefix) {
            this.channelPrefix = channelPrefix;
        }

        public String getChannelName(String symbol) {
            return channelPrefix + ":" + symbol;
        }
    }
}