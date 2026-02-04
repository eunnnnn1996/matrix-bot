package com.example.bithumb.strategy;

public interface TradeStrategy {
    TradeSignal decide(String coin, double currentPrice);
    void reset();
}

