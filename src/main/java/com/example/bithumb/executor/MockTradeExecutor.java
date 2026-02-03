package com.example.bithumb.executor;

import org.springframework.stereotype.Component;

@Component
public class MockTradeExecutor implements TradeExecutor {

    @Override
    public void buy(String coin, double price, double quantity) {
        System.out.printf(
            "[MOCK BUY] %s | price=%.0f | qty=%.6f%n",
            coin, price, quantity
        );
    }

    @Override
    public void sell(String coin, double price, double quantity) {
        System.out.printf(
            "[MOCK SELL] %s | price=%.0f | qty=%.6f%n",
            coin, price, quantity
        );
    }
}
