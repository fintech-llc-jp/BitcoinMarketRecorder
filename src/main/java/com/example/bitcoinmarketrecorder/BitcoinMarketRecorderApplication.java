package com.example.bitcoinmarketrecorder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BitcoinMarketRecorderApplication {

  public static void main(String[] args) {
    SpringApplication.run(BitcoinMarketRecorderApplication.class, args);
  }
}
