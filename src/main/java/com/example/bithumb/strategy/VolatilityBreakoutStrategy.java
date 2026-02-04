package com.example.bithumb.strategy;

import org.springframework.stereotype.Component;

import com.example.bithumb.service.CandleService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VolatilityBreakoutStrategy implements TradeStrategy {

    private final CandleService candleService;

    // 상태(최소)
    private String targetDate = "";
    private double targetPrice = 0;
    private boolean boughtToday = false;

    private final double k = 0.5;      // 튜닝값
    private final double quantity = 0.001;

    @Override
    public TradeSignal decide(String coin, double currentPrice) {
        if (currentPrice == 0) {
            return new TradeSignal(TradeSignal.Action.HOLD, 0, targetPrice, "price=0");
        }

        String today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).toString();

        // 날짜 변경 시 목표가 갱신
        if (!today.equals(targetDate)) {
            double open = candleService.getTodayOpen(coin);
            double range = candleService.getYesterdayRange(coin);

            targetPrice = open + range * k;
            targetDate = today;
            boughtToday = false;

            return new TradeSignal(TradeSignal.Action.HOLD, 0, targetPrice, "target updated");
        }

        // 매수 조건: 목표가 돌파 + 오늘 미매수
        if (!boughtToday && currentPrice >= targetPrice) {
            boughtToday = true;
            return new TradeSignal(TradeSignal.Action.BUY, quantity, targetPrice, "breakout");
        }

        return new TradeSignal(TradeSignal.Action.HOLD, 0, targetPrice, "no signal");
    }

    @Override
    public void reset() {
        targetDate = "";
        targetPrice = 0;
        boughtToday = false;
    }

}

