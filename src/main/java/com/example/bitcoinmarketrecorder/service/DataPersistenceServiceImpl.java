package com.example.bitcoinmarketrecorder.service;

import com.example.bitcoinmarketrecorder.model.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DataPersistenceServiceImpl implements DataPersistenceService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DataPersistenceServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void saveTrades(List<Trade> trades) {
        if (trades == null || trades.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO trades (exchange, symbol, trade_id, price, size, side, timestamp, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT (trade_id) DO NOTHING"; // Avoid duplicates based on trade_id

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Trade trade = trades.get(i);
                ps.setString(1, trade.getExchange());
                ps.setString(2, trade.getSymbol());
                ps.setString(3, trade.getTradeId());
                ps.setBigDecimal(4, trade.getPrice());
                ps.setBigDecimal(5, trade.getSize());
                ps.setString(6, trade.getSide());
                // Convert Instant to SQL Timestamp
                ps.setTimestamp(7, trade.getTimestamp() != null ? Timestamp.from(trade.getTimestamp()) : null);
                // Use current time for created_at if not set, otherwise use provided time
                ps.setTimestamp(8, trade.getCreatedAt() != null ? Timestamp.valueOf(trade.getCreatedAt()) : Timestamp.valueOf(LocalDateTime.now())); 
            }

            @Override
            public int getBatchSize() {
                return trades.size();
            }
        });
        // System.out.println("Saved " + trades.size() + " trades."); // Optional: logging
    }

    // Implementation for saveMarketBoard will go here later
} 