package com.example.bithumb.executor;

import java.util.List;
import java.util.Map;

public interface TradeExecutor {
    Map<String, Object> buy(String coin, double price, double quantity);
    Map<String, Object> sell(String coin, double price, double quantity);
    List<Map<String, Object>> getBalance();
}
