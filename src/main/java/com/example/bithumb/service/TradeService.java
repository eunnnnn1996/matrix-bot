package com.example.bithumb.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.bithumb.client.BithumbPrivateClient;
import com.example.bithumb.domain.TradeHistory;
import com.example.bithumb.executor.TradeExecutor;
import com.example.bithumb.repository.TradeHistoryRepository;
import com.example.bithumb.strategy.TradeSignal;
import com.example.bithumb.strategy.TradeStrategy;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final BithumbPrivateClient bithumbPrivateClient;
    private final TradeHistoryRepository tradeHistoryRepository;
    private final TradeExecutor tradeExecutor;
    private final TradeStrategy tradeStrategy;

    private volatile boolean botRunning = false;

    public void startBot() {
        botRunning = true;
        System.out.println("=== BOT STARTED ===");
        tradeStrategy.reset();
    }

    public void stopBot() {
        botRunning = false;
        System.out.println("=== BOT STOPPED ===");
    }

    public boolean isRunning() {
        return botRunning;
    }

    public void autoTradeJob() {
        if (!botRunning) return;
        executeAutoTrade("BTC");
    }

    // 메인 로직
    public void executeAutoTrade(String coin) {
        System.out.println("BOT tick");

        double current = bithumbPrivateClient.getCurrentPrice(coin);

        if (current == 0) return;
        TradeSignal signal = tradeStrategy.decide(coin, current);
        System.out.println("current=" + current + " target=" + signal.targetPrice() + " gap=" + (signal.targetPrice() - current));

        switch (signal.action()) {
            case BUY -> {
                tradeExecutor.buy(coin, current, signal.quantity());
                saveHistory(coin, "BUY", current, signal.reason());
            }
            case SELL -> {
                tradeExecutor.sell(coin, current, signal.quantity());
                saveHistory(coin, "SELL", current, signal.reason());
            }
            case HOLD -> {
                // 필요하면 로그만
                // System.out.println("HOLD: target=" + signal.targetPrice() + " reason=" + signal.reason());
            }
        }
    }

    public List<Map<String, Object>> getBalance() {
        return tradeExecutor.getBalance();
    }

    private void saveHistory(String coin, String side, double price, String reason) {
    }
}