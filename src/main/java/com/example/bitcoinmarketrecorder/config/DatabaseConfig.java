package com.example.bitcoinmarketrecorder.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
public class DatabaseConfig {

    // Configure DuckDB DataSource
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        // Specify the path for the DuckDB database file
        // Using ":memory:" will create an in-memory database
        dataSource.setUrl("jdbc:duckdb:market_data.duckdb"); 
        // No username/password needed for DuckDB file
        return dataSource;
    }

    // Configure JdbcTemplate
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // Initialize the database schema on startup
    @Bean
    public boolean initializeDatabase(DataSource dataSource) {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS trades (
                exchange VARCHAR,           -- 取引所（'GMO' or 'BITFLYER'）
                symbol VARCHAR,             -- 取引ペア（'BTC_JPY', 'FX_BTC_JPY'など）
                trade_id VARCHAR PRIMARY KEY, -- 約定ID (Primary Key)
                price DECIMAL(20,8),        -- 約定価格
                size DECIMAL(20,8),         -- 約定数量
                side VARCHAR,               -- 取引タイプ（'BUY' or 'SELL'）
                timestamp TIMESTAMP,        -- 約定時刻 (from WebSocket)
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- レコード作成時刻 (DB insertion time)
            )
            """;
        
        // Add table for MarketBoard data later if needed
        /*
        String createMarketBoardTableSql = """
            CREATE TABLE IF NOT EXISTS market_board (
                -- Define columns based on MarketBoard.java
                -- Example:
                -- id BIGINT AUTO_INCREMENT PRIMARY KEY,
                -- exchange VARCHAR,
                -- symbol VARCHAR,
                -- ts TIMESTAMP,
                -- bid1 DECIMAL(20,8),
                -- ... other bids/asks/volumes ...
                -- created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        */

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createTableSql);
            // statement.execute(createMarketBoardTableSql); // Uncomment when MarketBoard table is needed
            System.out.println("Database table 'trades' initialized successfully.");
            return true;
        } catch (SQLException e) {
            System.err.println("Error initializing database schema: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
} 