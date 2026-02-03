package com.example.bithumb.executor;

public interface TradeExecutor {
    void buy(String coin, double price, double quantity);
    void sell(String coin, double price, double quantity);
}
