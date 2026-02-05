package com.example.bithumb.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.bithumb.client.BithumbPrivateClient;
import com.example.bithumb.domain.TradeHistory;
import com.example.bithumb.domain.TradeLog;
import com.example.bithumb.executor.TradeExecutor;
import com.example.bithumb.repository.TradeHistoryRepository;
import com.example.bithumb.repository.TradeLogRepository;
import com.example.bithumb.strategy.TradeSignal;
import com.example.bithumb.strategy.TradeStrategy;
import com.example.bithumb.domain.BalanceSnapshot;
import com.example.bithumb.repository.BalanceSnapshotRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final BithumbPrivateClient bithumbPrivateClient;
    private final TradeHistoryRepository tradeHistoryRepository;
    private final TradeExecutor tradeExecutor;
    private final TradeStrategy tradeStrategy;
    private final TradeLogRepository tradeLogRepository;
    private final BalanceSnapshotRepository balanceSnapshotRepository;

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

    // 메인 로직
    public void executeAutoTrade(String coin) {
        System.out.println("BOT tick");
        double currentPrice = bithumbPrivateClient.getCurrentPrice(coin);
        if (currentPrice == 0) return;

        TradeSignal signal = tradeStrategy.decide(coin, currentPrice);

        // 🔽 여기서 분기한다
        if (signal.action() == TradeSignal.Action.BUY) {

            tradeExecutor.buy(coin, currentPrice, signal.quantity());

            tradeLogRepository.save(
                new TradeLog(
                    coin,
                    "BUY",
                    currentPrice,
                    signal.quantity(),
                    tradeStrategy.getClass().getSimpleName(),
                    signal.reason()
                )
            );
            saveBalanceSnapshot(coin);
        } else if (signal.action() == TradeSignal.Action.SELL) {

            tradeExecutor.sell(coin, currentPrice, signal.quantity());

            tradeLogRepository.save(
                new TradeLog(
                    coin,
                    "SELL",
                    currentPrice,
                    signal.quantity(),
                    tradeStrategy.getClass().getSimpleName(),
                    signal.reason()
                )
            );
            saveBalanceSnapshot(coin);
        }
    }


    public List<Map<String, Object>> getBalance() {
        return tradeExecutor.getBalance();
    }

    private void saveBalanceSnapshot(String coin) {
    try {
        // 빗썸 계좌 리스트
        var balances = tradeExecutor.getBalance(); // == privateClient.getBalance()

        Long krwBal = null;
        Double coinBal = null;
        Long avgBuy = null;

        for (var row : balances) {
            String currency = String.valueOf(row.getOrDefault("currency", "")).toUpperCase();

            // KRW 잔고
            if ("KRW".equals(currency)) {
                krwBal = parseLongSafe(row.get("balance"));
            }

            // 코인 잔고 (예: BTC / unit_currency = KRW)
            if (coin.equalsIgnoreCase(currency)) {
                coinBal = parseDoubleSafe(row.get("balance"));
                avgBuy = parseLongSafe(row.get("avg_buy_price")); // 있으면 저장
            }
        }

        long currentPx = Math.round(bithumbPrivateClient.getCurrentPrice(coin));

        balanceSnapshotRepository.save(
                new BalanceSnapshot(coin, krwBal, coinBal, avgBuy, currentPx)
        );

    } catch (Exception e) {
        System.out.println("[BAL_SNAPSHOT_FAIL] " + e.getMessage());
    }
}

        private Long parseLongSafe(Object v) {
            if (v == null) return null;
            try {
                // "0", "123.0" 같이 올 수도 있어서 double로 한번 처리
                double d = Double.parseDouble(v.toString());
                return (long) Math.floor(d);
            } catch (Exception e) {
                return null;
            }
        }

        private Double parseDoubleSafe(Object v) {
            if (v == null) return null;
            try {
                return Double.parseDouble(v.toString());
            } catch (Exception e) {
                return null;
            }
        }

    
}
