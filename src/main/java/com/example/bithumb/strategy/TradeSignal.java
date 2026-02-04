package com.example.bithumb.strategy;

public record TradeSignal(
        Action action,       // BUY, SELL, HOLD
        double quantity,
        double targetPrice,
        String reason
) {
    public enum Action { BUY, SELL, HOLD }
}
