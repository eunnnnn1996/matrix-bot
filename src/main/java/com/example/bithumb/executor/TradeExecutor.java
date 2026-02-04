package com.example.bithumb.executor;

import java.util.List;
import java.util.Map;

public interface TradeExecutor {
    void buy(String coin, double price, double quantity);
    void sell(String coin, double price, double quantity);
    List<Map<String, Object>> getBalance();
}
