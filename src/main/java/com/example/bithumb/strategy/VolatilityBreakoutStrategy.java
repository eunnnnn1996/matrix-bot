package com.example.bithumb.strategy;

import org.springframework.stereotype.Component;

import com.example.bithumb.service.CandleService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VolatilityBreakoutStrategy implements TradeStrategy {

    private final CandleService candleService;

    private String targetDate = "";
    private double targetPrice = 0;

    private boolean boughtToday = false;
    private double entryPrice = 0;              // 추가: 매수가

    private final double k = 0.002;             // 매수 목표가 튜닝
    private final double takeProfit = 0.002;    // 추가: 익절 0.2%
    private final double stopLoss = 0.002;      // 추가: 손절 0.2%
    private final double quantity = 0.00001;

    @Override
    public TradeSignal decide(String coin, double currentPrice) {
        if (currentPrice == 0) {
            return new TradeSignal(TradeSignal.Action.HOLD, 0, targetPrice, "price=0");
        }

        String today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).toString();

        // 날짜 변경 시 목표가 갱신 + 상태 리셋
        if (!today.equals(targetDate)) {
            double open = candleService.getTodayOpen(coin);
            double range = candleService.getYesterdayRange(coin);

            targetPrice = open + range * k;
            targetDate = today;

            boughtToday = false;
            entryPrice = 0;

            return new TradeSignal(TradeSignal.Action.HOLD, 0, targetPrice, "target updated");
        }

        // 매수 조건: 목표가 돌파 + 오늘 미매수
        if (!boughtToday && currentPrice >= targetPrice) {
            boughtToday = true;
            entryPrice = currentPrice; // 매수가 기록
            return new TradeSignal(TradeSignal.Action.BUY, quantity, targetPrice, "breakout");
        }

        // 매도 조건: 매수가 기준 익절/손절
        if (boughtToday && entryPrice > 0) {
            double tpPrice = entryPrice * (1 + takeProfit);
            double slPrice = entryPrice * (1 - stopLoss);

            if (currentPrice >= tpPrice) {
                boughtToday = false;   // 오늘 재진입 막고 싶으면 true 유지해도 됨
                entryPrice = 0;
                return new TradeSignal(TradeSignal.Action.SELL, quantity, targetPrice, "take profit");
            }
            if (currentPrice <= slPrice) {
                boughtToday = false;
                entryPrice = 0;
                return new TradeSignal(TradeSignal.Action.SELL, quantity, targetPrice, "stop loss");
            }
        }

        return new TradeSignal(TradeSignal.Action.HOLD, 0, targetPrice, "no signal");
    }

    @Override
    public void reset() {
        targetDate = "";
        targetPrice = 0;
        boughtToday = false;
        entryPrice = 0;
    }
}
