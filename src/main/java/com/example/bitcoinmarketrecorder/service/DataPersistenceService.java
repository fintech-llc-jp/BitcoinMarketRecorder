package com.example.bitcoinmarketrecorder.service;

import com.example.bitcoinmarketrecorder.model.BestBidAsk;
import com.example.bitcoinmarketrecorder.model.MarketBoard;
import com.example.bitcoinmarketrecorder.model.Trade;
import java.util.List;

public interface DataPersistenceService {
  void saveTrades(List<Trade> trades);

  void saveMarketBoard(MarketBoard board);

  void saveBestBidAsk(BestBidAsk bestBidAsk);
}
