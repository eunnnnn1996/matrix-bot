package com.example.bithumb.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
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

    @Value("${trade.mode:paper}")
    private String tradeMode;

    private volatile
    boolean botRunning = false;

    private static final String MARKET = "KRW-BTC";
    private static final String COIN = "BTC";
    private static final long OPEN_ORDER_TIMEOUT_SEC = 60;

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
            // 1) 미체결 주문 확인 (live 전용)
            if ("live".equalsIgnoreCase(tradeMode)) {
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

    // ===== uuid로 주문 완료 감지 (live 전용) =====
    private void refreshOpenOrderIfFinished() {
        if (!"live".equalsIgnoreCase(tradeMode)) return;

        if (!botState.isHasOpenOrder()) return;
        String uuid = botState.getOpenOrderUuid();
        if (uuid == null || uuid.isBlank()) return;

        // ✅ 60초 초과면 자동취소 시도
        Instant createdAt = botState.getOpenOrderCreatedAt();
        if (createdAt != null) {
            long elapsed = Duration.between(createdAt, Instant.now()).getSeconds();
            if (elapsed >= OPEN_ORDER_TIMEOUT_SEC) {
                System.out.println("[ORDER_TIMEOUT] uuid=" + uuid + " elapsedSec=" + elapsed + " -> cancel");

                Map<String, Object> cancelRes = bithumbPrivateClient.cancelOrder(uuid);
                System.out.println("[ORDER_CANCEL_RES] uuid=" + uuid + " res=" + cancelRes);

                // 취소 시도 후 오픈오더 플래그는 내려줌(재조회로 다시 잡힐 수 있음)
                botState.setHasOpenOrder(false);
                botState.setOpenOrderUuid(null);
                botState.setOpenOrderSide(null);
                botState.setOpenOrderCreatedAt(null);

                // 잔고 다시 동기화
                syncPositionOnly();
                return;
            }
        }

        // 아직 타임아웃 전이면 상태 조회
        Map<String, Object> order = bithumbPrivateClient.getOrder(uuid);
        if (order == null || order.isEmpty()) return;

        String state = String.valueOf(order.getOrDefault("state", "")).toLowerCase(); // wait / done / cancel
        String remaining = String.valueOf(order.getOrDefault("remaining_volume", ""));

        boolean finishedByState = state.equals("done") || state.equals("cancel");
        boolean finishedByRemaining = false;

        try {
            if (remaining != null && !remaining.isBlank()) {
                finishedByRemaining = Double.parseDouble(remaining) == 0.0;
            }
        } catch (Exception ignored) {}

        if (finishedByState || finishedByRemaining) {
            System.out.println("[ORDER_FINISHED] uuid=" + uuid + " state=" + state + " remaining=" + remaining);

            botState.setHasOpenOrder(false);
            botState.setOpenOrderUuid(null);
            botState.setOpenOrderSide(null);
            botState.setOpenOrderCreatedAt(null);

            syncPositionOnly();
        } else {
            System.out.println("[ORDER_WAIT] uuid=" + uuid + " state=" + state + " remaining=" + remaining);
        }
    }


    // ===== 포지션(잔고)만 동기화 (paper/live 공통) =====
    private void syncPositionOnly() {
        try {
            var balances = tradeExecutor.getBalance();
            double btc = 0.0;
            Double avgBuy = null;

            for (var row : balances) {
                String currency = String.valueOf(row.getOrDefault("currency", "")).toUpperCase();
                if ("BTC".equals(currency)) {
                    btc = parseDoubleSafe(row.get("balance"));

                    double avg = parseDoubleSafe(row.get("avg_buy_price"));
                    if (avg > 0) avgBuy = avg;

                    break;
                }
            }

            botState.setHasPosition(btc > 0.0);
            botState.setPositionVolume(java.math.BigDecimal.valueOf(btc));
            botState.setPositionAvgBuyPrice(avgBuy); // ✅ 이 줄이 핵심
        } catch (Exception e) {
            System.out.println("[POS_SYNC_FAIL] " + e.getMessage());
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
        if (!botRunning) return;
        System.out.println("BOT tick");

        // ✅ live면 uuid 주문 상태 확인해서 완료되면 hasOpenOrder 내려줌
        refreshOpenOrderIfFinished();

        // ✅ 포지션은 매 tick마다 동기화(paper 무한매수/상태정합)
        syncPositionOnly();

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
                botState.setOpenOrderCreatedAt(Instant.now()); // ✅ 추가
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
                botState.setOpenOrderCreatedAt(Instant.now()); // ✅ 추가
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

        // ✅ 주문 상태는 tick마다 uuid로 확인(최소 기능)
        // "미체결 사라짐 -> hasOpenOrder=false"는 refreshOpenOrderIfFinished()가 담당
        // 보유량 변화는 syncPositionOnly()가 담당
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
