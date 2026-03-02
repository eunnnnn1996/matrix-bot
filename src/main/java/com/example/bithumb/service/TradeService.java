package com.example.bithumb.service;

import com.example.bithumb.client.BithumbPrivateClient;
import com.example.bithumb.domain.BalanceSnapshot;
import com.example.bithumb.domain.TradeLog;
import com.example.bithumb.executor.TradeExecutor;
import com.example.bithumb.notifier.SlackNotifier;
import com.example.bithumb.repository.BalanceSnapshotRepository;
import com.example.bithumb.repository.TradeLogRepository;
import com.example.bithumb.strategy.TradeSignal;
import com.example.bithumb.strategy.TradeStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final BithumbPrivateClient bithumbPrivateClient;
    private final TradeExecutor tradeExecutor;
    private final TradeStrategy tradeStrategy;
    private final TradeLogRepository tradeLogRepository;
    private final BalanceSnapshotRepository balanceSnapshotRepository;

    private final BotState botState;

    // settings 서비스 (캐시 사용)
    private final BotSettingsService botSettingsService;
    private final EventService eventService;
    private final SlackNotifier slackNotifier;

    // ===== SELL 후 바로 BUY 재진입 방지 =====
    private long lastTradeAtMs = 0L;
    private static final long COOLDOWN_MS = 30_000; // 30초

    @Value("${trade.mode:paper}")
    private String tradeMode;

    private volatile boolean botRunning = false;

    private static final long OPEN_ORDER_TIMEOUT_SEC = 60;

    // ===============================
    // settings helper
    // ===============================
    private String getCoin() {
        return botSettingsService.botSelect().getCoin();
    }

    private String getMarket() {
        return "KRW-" + getCoin();
    }

    public void startBot() {
        botRunning = true;
        System.out.println("=== BOT STARTED ===");
        tradeStrategy.reset();
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

        String coin = getCoin();
        String market = getMarket();

        try {
            if ("live".equalsIgnoreCase(tradeMode)) {

                List<Map<String, Object>> openOrders =
                        bithumbPrivateClient.getOpenOrders(market);

                if (openOrders != null && !openOrders.isEmpty()) {
                    Map<String, Object> o = openOrders.get(0);
                    botState.setHasOpenOrder(true);
                    botState.setOpenOrderUuid(String.valueOf(o.get("uuid")));
                    botState.setOpenOrderSide(
                            o.get("side") == null ? null : String.valueOf(o.get("side")));
                } else {
                    botState.setHasOpenOrder(false);
                    botState.setOpenOrderUuid(null);
                    botState.setOpenOrderSide(null);
                }
            } else {
                botState.setHasOpenOrder(false);
                botState.setOpenOrderUuid(null);
                botState.setOpenOrderSide(null);
            }

            var balances = tradeExecutor.getBalance();
            double coinBal = 0.0;

            for (var row : balances) {
                String currency =
                        String.valueOf(row.getOrDefault("currency", "")).toUpperCase();

                if (coin.equalsIgnoreCase(currency)) {
                    coinBal = parseDoubleSafe(row.get("balance"));
                    break;
                }
            }

            botState.setHasPosition(coinBal > 0.0);
            botState.setPositionVolume(java.math.BigDecimal.valueOf(coinBal));

        } catch (Exception e) {
            System.out.println("[SYNC_FAIL] " + e.getMessage());
        }

        // ===============================
        // ⭐ 마지막 TradeLog 기준 포지션 복구
        // ===============================
        try {

            tradeLogRepository
                    .findTopByCoinOrderByCreatedAtDesc(coin)
                    .ifPresent(last -> {

                        if ("BUY".equalsIgnoreCase(last.getSide())) {

                            botState.setHasPosition(true);
                            botState.setPositionVolume(
                                    java.math.BigDecimal.valueOf(last.getQty())
                            );
                            botState.setPositionAvgBuyPrice(last.getPrice());
                            tradeExecutor.buy(coin, last.getPrice(), last.getQty() );
                            System.out.println("[RESTORE] position restored");

                        } else {
                            
                            botState.setHasPosition(false);
                            botState.setPositionVolume(java.math.BigDecimal.ZERO);
                            botState.setPositionAvgBuyPrice(null);

                            System.out.println("[RESTORE] no position");
                        }
                    });

        } catch (Exception e) {
            System.out.println("[RESTORE_FAIL] " + e.getMessage());
        }
    }

    // ===== uuid 주문 완료 감지 =====
    private void refreshOpenOrderIfFinished() {

        if (!"live".equalsIgnoreCase(tradeMode)) return;
        if (!botState.isHasOpenOrder()) return;

        String uuid = botState.getOpenOrderUuid();
        if (uuid == null || uuid.isBlank()) return;

        Instant createdAt = botState.getOpenOrderCreatedAt();

        if (createdAt != null) {
            long elapsed =
                    Duration.between(createdAt, Instant.now()).getSeconds();

            if (elapsed >= OPEN_ORDER_TIMEOUT_SEC) {

                System.out.println("[ORDER_TIMEOUT] uuid=" + uuid);

                bithumbPrivateClient.cancelOrder(uuid);

                botState.setHasOpenOrder(false);
                botState.setOpenOrderUuid(null);
                botState.setOpenOrderSide(null);
                botState.setOpenOrderCreatedAt(null);

                return;
            }
        }

        Map<String, Object> order =
                bithumbPrivateClient.getOrder(uuid);

        if (order == null || order.isEmpty()) return;

        String state =
                String.valueOf(order.getOrDefault("state", "")).toLowerCase();

        String remaining =
                String.valueOf(order.getOrDefault("remaining_volume", ""));

        boolean finished =
                state.equals("done") ||
                        state.equals("cancel");

        try {
            if (remaining != null && !remaining.isBlank()) {
                finished |= Double.parseDouble(remaining) == 0.0;
            }
        } catch (Exception ignored) {}

        if (finished) {
            botState.setHasOpenOrder(false);
            botState.setOpenOrderUuid(null);
            botState.setOpenOrderSide(null);
            botState.setOpenOrderCreatedAt(null);
        }
    }

    // ===== 포지션 동기화 =====
    private void syncPositionOnly() {

        String coin = getCoin();

        try {
            var balances = tradeExecutor.getBalance();

            double coinBal = 0.0;
            Double avgBuy = null;

            for (var row : balances) {

                String currency =
                        String.valueOf(row.getOrDefault("currency", "")).toUpperCase();

                if (coin.equalsIgnoreCase(currency)) {

                    coinBal = parseDoubleSafe(row.get("balance"));

                    double avg = parseDoubleSafe(row.get("avg_buy_price"));
                    if (avg > 0) avgBuy = avg;

                    break;
                }
            }

            botState.setHasPosition(coinBal > 0.0);
            botState.setPositionVolume(java.math.BigDecimal.valueOf(coinBal));
            botState.setPositionAvgBuyPrice(avgBuy);

        } catch (Exception e) {
            System.out.println("[POS_SYNC_FAIL] " + e.getMessage());
        }
    }

    // ===== 메인 로직 =====
    public void executeAutoTrade() {
        
        if (!botRunning) return;

        String coin = getCoin();

        System.out.println("BOT tick");
        refreshOpenOrderIfFinished();

        // ===== 상태 동기화 =====
        if (!botState.isHasPosition()) {
            syncPositionOnly();
        }

        double currentPrice = bithumbPrivateClient.getCurrentPrice(coin);
        if (currentPrice == 0) return;
        // ===== 전략 신호 =====
        TradeSignal signal = tradeStrategy.decide(coin, currentPrice);
        System.out.println("[SIGNAL] " + signal.action() + " reason=" + signal.reason());
        // ===== 익절/손절 우선 체크 (보유중일 때만) =====
        if (botState.isHasPosition()) {

            Double avgBuyObj = botState.getPositionAvgBuyPrice();
            if (avgBuyObj != null && avgBuyObj > 0) {

                double avgBuy = avgBuyObj;

                double tpRate = botSettingsService.botSelect().getTakeProfit();
                double slRate = botSettingsService.botSelect().getStopLoss();

                double tp = avgBuy * (1 + tpRate);
                double sl = avgBuy * (1 - slRate);

                double qtyToSell = botState.getPositionVolume() == null ? 0.0 : botState.getPositionVolume().doubleValue();

                if (qtyToSell > 0 && (currentPrice >= tp || currentPrice <= sl)) {

                    tradeExecutor.sell(coin, currentPrice, qtyToSell);
                    botState.setHasPosition(false);
                    botState.setPositionVolume(java.math.BigDecimal.ZERO);
                    botState.setPositionAvgBuyPrice(null);

                    TradeLog sellLog = new TradeLog(
                            coin,
                            "SELL",
                            currentPrice,
                            qtyToSell,
                            "FORCE_SELL",
                            "tp/sl"
                    );

                    // ⭐⭐⭐ 이거 추가 (핵심)
                    double realizedPnl =
                            (currentPrice - avgBuyObj) * qtyToSell;

                    sellLog.setAvgBuyAtTrade(avgBuyObj);
                    sellLog.setRealizedPnl(realizedPnl);

                    tradeLogRepository.save(sellLog);
                    saveBalanceSnapshot(coin);
                    eventService.eventSave(
                            "ORDER",
                            "SUCCESS",
                            coin,
                            "SELL",
                            currentPrice,
                            signal.quantity(),
                            "[주문] " + coin + " 강제매도 " + signal.quantity()
                    );
                    syncPositionOnly();
                    // ✅ 쿨다운 시작 + 메서드 종료
                    lastTradeAtMs = System.currentTimeMillis();

                    System.out.println("[FORCE SELL] price=" + currentPrice + " qty=" + qtyToSell);
                    slackNotifier.send("🧊 [강제매도] " + coin + " px=" + currentPrice + " qty=" + qtyToSell);
                    return;
                }
            }
        }

        // ===== 쿨다운 =====
        long now = System.currentTimeMillis();
        if (now - lastTradeAtMs < COOLDOWN_MS) {
            System.out.println("[COOLDOWN] skip tick");
            return;
        }

        // ===== 보유중인데 BUY 신호면 무시 =====
        if (botState.isHasPosition() && signal.action() == TradeSignal.Action.BUY) {
            System.out.println("[GUARD] already holding -> ignore BUY");
            return;
        }

        // ===== BUY =====
        if (signal.action() == TradeSignal.Action.BUY) {

            Map<String,Object> result =
        tradeExecutor.buy(coin, currentPrice, signal.quantity());

        // ❗ BUY 실패하면 여기서 끝
        if (!(Boolean) result.getOrDefault("success", false)) {
            System.out.println("[BUY FAIL] " + result);
            return;
        }

        syncPositionOnly();
        lastTradeAtMs = System.currentTimeMillis();

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

            eventService.eventSave(
                    "ORDER",
                    "SUCCESS",
                    coin,
                    "BUY",
                    currentPrice,
                    signal.quantity(),
                    "[주문] " + coin + " 매수 " + signal.quantity()
            );

            slackNotifier.send("🔥 [매수] " + coin + " px=" + currentPrice + " 수량=" + signal.quantity());
            return;
        }

        // ===== SELL =====
        if (signal.action() == TradeSignal.Action.SELL) {

            // SELL 전에 평균매수가 확보
            Double avgBuy = botState.getPositionAvgBuyPrice();

            Map<String,Object> result =
                    tradeExecutor.sell(coin, currentPrice, signal.quantity());

            // ❗ SELL 실패하면 종료
            if (!(Boolean) result.getOrDefault("success", false)) {
                System.out.println("[SELL FAIL] " + result);
                return;
            }

            syncPositionOnly();
            lastTradeAtMs = System.currentTimeMillis();

            TradeLog sellLog = new TradeLog(
                    coin,
                    "SELL",
                    currentPrice,
                    signal.quantity(),
                    tradeStrategy.getClass().getSimpleName(),
                    signal.reason()
            );

            if (avgBuy != null && avgBuy > 0) {
                double realizedPnl =
                        (currentPrice - avgBuy) * signal.quantity();

                sellLog.setAvgBuyAtTrade(avgBuy);
                sellLog.setRealizedPnl(realizedPnl);
            }

            tradeLogRepository.save(sellLog);

            saveBalanceSnapshot(coin);

            eventService.eventSave(
                    "ORDER",
                    "SUCCESS",
                    coin,
                    "SELL",
                    currentPrice,
                    signal.quantity(),
                    "[주문] " + coin + " 매도 " + signal.quantity()
            );

            slackNotifier.send("🔥 [매도] " + coin + " px=" + currentPrice + " qty=" + signal.quantity());
        }
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

                String currency =
                        String.valueOf(row.getOrDefault("currency", "")).toUpperCase();

                if ("KRW".equals(currency))
                    krwBal = parseLongSafe(row.get("balance"));

                if (coin.equalsIgnoreCase(currency)) {
                    coinBal = parseDoubleSafe(row.get("balance"));
                    avgBuy = parseLongSafe(row.get("avg_buy_price"));
                }
            }

            long currentPx =
                    Math.round(bithumbPrivateClient.getCurrentPrice(coin));

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
            return (long) Math.floor(Double.parseDouble(v.toString()));
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