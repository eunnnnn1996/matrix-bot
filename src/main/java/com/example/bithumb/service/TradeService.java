package com.example.bithumb.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.bithumb.client.BithumbClient;
import com.example.bithumb.domain.TradeHistory;
import com.example.bithumb.executor.TradeExecutor;
import com.example.bithumb.repository.TradeHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final BithumbClient bithumbClient;
    private final TradeHistoryRepository tradeHistoryRepository;
    private final TradeExecutor tradeExecutor;

    private volatile boolean botRunning = false;
    private double lastPrice = 0;

    /* ===== 봇 제어 ===== */
    public void startBot() {
        botRunning = true;
        System.out.println("=== BOT STARTED ===");
    }

    public void stopBot() {
        botRunning = false;
        System.out.println("=== BOT STOPPED ===");
    }

    public boolean isRunning() {
        return botRunning;
    }

    /* ===== 자동매매 루프 ===== */
    @Scheduled(fixedDelay = 3000)
    public void autoTradeJob() {
        if (!botRunning) return;
        System.out.println("BOT tick");
        executeAutoTrade("BTC");
    }

    /* ===== 매매 로직 ===== */
    public void executeAutoTrade(String coin) {
        double current = bithumbClient.getCurrentPrice(coin);
        if (current == 0) return;

        if (lastPrice == 0) {
            lastPrice = current;
            System.out.println("기준가 설정: " + current);
            return;
        }

        // 💡 테스트용 고정 수량 (나중에 여기만 바꾸면 됨)
        double quantity = 0.001;

        if (current < lastPrice * 0.995) {
            tradeExecutor.buy(coin, current, quantity);
            saveHistory(coin, "BUY", current);
        } 
        else if (current > lastPrice * 1.005) {
            tradeExecutor.sell(coin, current, quantity);
            saveHistory(coin, "SELL", current);
        }

        lastPrice = current;
    }

    private void saveHistory(String coin, String type, double price) {
        TradeHistory history = TradeHistory.builder()
                .coin(coin)
                .tradeType(type)
                .price(price)
                .tradedAt(LocalDateTime.now())
                .build();
        tradeHistoryRepository.save(history);
    }

    /* ===== 잔고 조회 ===== */
    public List<Map<String, Object>> getBalance() {
        return bithumbClient.getBalance();
    }
}
