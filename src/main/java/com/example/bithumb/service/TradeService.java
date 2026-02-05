package com.example.bithumb.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.bithumb.client.BithumbPrivateClient;
import com.example.bithumb.domain.TradeLog;
import com.example.bithumb.executor.TradeExecutor;
import com.example.bithumb.repository.BalanceSnapshotRepository;
import com.example.bithumb.repository.TradeHistoryRepository;
import com.example.bithumb.repository.TradeLogRepository;
import com.example.bithumb.strategy.TradeSignal;
import com.example.bithumb.strategy.TradeStrategy;
import com.example.bithumb.domain.BalanceSnapshot;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final BithumbPrivateClient bithumbPrivateClient;
    private final TradeHistoryRepository tradeHistoryRepository; // 현재 코드에 있어 유지
    private final TradeExecutor tradeExecutor;
    private final TradeStrategy tradeStrategy;
    private final TradeLogRepository tradeLogRepository;
    private final BalanceSnapshotRepository balanceSnapshotRepository;

    private final BotState botState;

    private volatile boolean botRunning = false;

    private static final String MARKET = "KRW-BTC";
    private static final String COIN = "BTC";

    public void startBot() {
        botRunning = true;
        System.out.println("=== BOT STARTED ===");

        // 전략 메모리 초기화(기존 유지)
        tradeStrategy.reset();

        // ✅ 재시작 안전 동기화(핵심)
        syncStateOnStart();
    }

    public void stopBot() {
        botRunning = false;
        System.out.println("=== BOT STOPPED ===");
    }

    public boolean isRunning() {
        return botRunning;
    }

    // ===== 재시작 동기화 =====
    private void syncStateOnStart() {
        try {
            // 1) 미체결 주문 확인
            List<Map<String, Object>> openOrders = bithumbPrivateClient.getOpenOrders(MARKET);
            if (openOrders != null && !openOrders.isEmpty()) {
                Map<String, Object> o = openOrders.get(0);
                botState.setHasOpenOrder(true);
                botState.setOpenOrderUuid(String.valueOf(o.get("uuid")));
                botState.setOpenOrderSide(o.get("side") == null ? null : String.valueOf(o.get("side")));
            } else {
                botState.setHasOpenOrder(false);
                botState.setOpenOrderUuid(null);
                botState.setOpenOrderSide(null);
            }

            // 2) 보유 수량 확인
            var balances = tradeExecutor.getBalance();
            double btc = 0.0;

            for (var row : balances) {
                String currency = String.valueOf(row.getOrDefault("currency", "")).toUpperCase();
                if ("BTC".equals(currency)) {
                    btc = parseDoubleSafe(row.get("balance"));
                    break;
                }
            }

            botState.setHasPosition(btc > 0.0);
            botState.setPositionVolume(java.math.BigDecimal.valueOf(btc));

            System.out.println("[SYNC] hasOpenOrder=" + botState.isHasOpenOrder()
                    + " uuid=" + botState.getOpenOrderUuid()
                    + " side=" + botState.getOpenOrderSide()
                    + " hasPosition=" + botState.isHasPosition()
                    + " btc=" + botState.getPositionVolume());
        } catch (Exception e) {
            System.out.println("[SYNC_FAIL] " + e.getMessage());
        }
    }

    // ===== 매수 가능 여부(중복매수 방지) =====
    private boolean canBuyNow() {
        // 미체결 주문 있으면 신규 주문 금지
        if (botState.isHasOpenOrder()) return false;

        // 이미 BTC 보유 중이면 매수 금지(분할매수 하고 싶으면 여기 정책만 바꾸면 됨)
        if (botState.isHasPosition()) return false;

        // 쿨다운(예: 60초)
        Instant last = botState.getLastBuyAt();
        if (last != null && Duration.between(last, Instant.now()).getSeconds() < 60) return false;

        return true;
    }

    // ===== 매도 가능 여부(중복매도 방지) =====
    private boolean canSellNow() {
        if (botState.isHasOpenOrder()) return false;
        if (!botState.isHasPosition()) return false;

        Instant last = botState.getLastSellAt();
        if (last != null && Duration.between(last, Instant.now()).getSeconds() < 60) return false;

        return true;
    }

    // ===== 메인 로직 =====
    public void executeAutoTrade(String coin) {
        System.out.println("BOT tick");
        double currentPrice = bithumbPrivateClient.getCurrentPrice(coin);
        if (currentPrice == 0) return;

        TradeSignal signal = tradeStrategy.decide(coin, currentPrice);

        if (signal.action() == TradeSignal.Action.BUY) {
            if (!canBuyNow()) {
                System.out.println("[SKIP BUY] hasOpenOrder=" + botState.isHasOpenOrder()
                        + " hasPosition=" + botState.isHasPosition());
                return;
            }

            Map<String, Object> res = tradeExecutor.buy(coin, currentPrice, signal.quantity());

            // ✅ 실주문 접수(uuid)면 오픈오더 플래그 세팅
            Object uuid = res.get("uuid");
            if (uuid != null) {
                botState.setHasOpenOrder(true);
                botState.setOpenOrderUuid(String.valueOf(uuid));
                botState.setOpenOrderSide("bid");
                botState.setLastBuyAt(Instant.now());
            }

            tradeLogRepository.save(
                    new TradeLog(
                            coin, "BUY", currentPrice, signal.quantity(),
                            tradeStrategy.getClass().getSimpleName(),
                            signal.reason()
                    )
            );
            saveBalanceSnapshot(coin);

        } else if (signal.action() == TradeSignal.Action.SELL) {
            if (!canSellNow()) {
                System.out.println("[SKIP SELL] hasOpenOrder=" + botState.isHasOpenOrder()
                        + " hasPosition=" + botState.isHasPosition());
                return;
            }

            Map<String, Object> res = tradeExecutor.sell(coin, currentPrice, signal.quantity());

            Object uuid = res.get("uuid");
            if (uuid != null) {
                botState.setHasOpenOrder(true);
                botState.setOpenOrderUuid(String.valueOf(uuid));
                botState.setOpenOrderSide("ask");
                botState.setLastSellAt(Instant.now());
            }

            tradeLogRepository.save(
                    new TradeLog(
                            coin, "SELL", currentPrice, signal.quantity(),
                            tradeStrategy.getClass().getSimpleName(),
                            signal.reason()
                    )
            );
            saveBalanceSnapshot(coin);
        }

        // ⚠️ 최소 버전: 체결/미체결 변화는 start에서만 동기화
        // 실전에서는 N초마다 openOrders/balance 재조회해서
        // "미체결 사라짐 -> hasOpenOrder=false", "보유 생김 -> hasPosition=true" 갱신하는게 맞음.
    }

    public List<Map<String, Object>> getBalance() {
        return tradeExecutor.getBalance();
    }

    private void saveBalanceSnapshot(String coin) {
        try {
            var balances = tradeExecutor.getBalance();

            Long krwBal = null;
            Double coinBal = null;
            Long avgBuy = null;

            for (var row : balances) {
                String currency = String.valueOf(row.getOrDefault("currency", "")).toUpperCase();

                if ("KRW".equals(currency)) {
                    krwBal = parseLongSafe(row.get("balance"));
                }
                if (coin.equalsIgnoreCase(currency)) {
                    coinBal = parseDoubleSafe(row.get("balance"));
                    avgBuy = parseLongSafe(row.get("avg_buy_price"));
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
            double d = Double.parseDouble(v.toString());
            return (long) Math.floor(d);
        } catch (Exception e) {
            return null;
        }
    }

    private double parseDoubleSafe(Object v) {
        if (v == null) return 0.0;
        try {
            return Double.parseDouble(v.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }
}
