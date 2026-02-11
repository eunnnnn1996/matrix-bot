package com.example.bithumb.strategy;

import org.springframework.stereotype.Component;

import com.example.bithumb.service.CandleService;
import com.example.bithumb.service.BotState;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VolatilityBreakoutStrategy implements TradeStrategy {

    private final CandleService candleService;
    private final BotState botState;

    private String targetDate = "";
    private double targetPrice = 0;

    private final double k = 0.002;             // 매수 목표가 튜닝
    private final double takeProfit = 0.0008;    // 익절 0.2%
    private final double stopLoss = 0.0008;      // 손절 0.2%
    private final double quantity = 0.007;

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

            return new TradeSignal(TradeSignal.Action.HOLD, 0, targetPrice, "target updated");
        }

        // ✅ 보유 여부/매수가 기준은 "전략 내부 메모리"가 아니라 거래소 동기화 상태(BotState)를 사용
        boolean hasPosition = botState.isHasPosition();
        Double avgBuy = botState.getPositionAvgBuyPrice();

        // 매수 조건: 목표가 돌파 + 미보유
        if (!hasPosition && currentPrice >= targetPrice) {
            return new TradeSignal(TradeSignal.Action.BUY, quantity, targetPrice, "breakout");
        }

        // 매도 조건: 평균매수가 기준 익절/손절 + 보유중
        if (hasPosition && avgBuy != null && avgBuy > 0) {
            double tpPrice = avgBuy * (1 + takeProfit);
            double slPrice = avgBuy * (1 - stopLoss);

            // 가능하면 보유수량 전량 매도(부분매도 원하면 이 정책만 변경)
            double posQty = botState.getPositionVolume() == null ? 0.0 : botState.getPositionVolume().doubleValue();
            double sellQty = posQty > 0 ? posQty : quantity;

            if (currentPrice >= tpPrice) {
                return new TradeSignal(TradeSignal.Action.SELL, sellQty, targetPrice, "take profit");
            }
            if (currentPrice <= slPrice) {
                return new TradeSignal(TradeSignal.Action.SELL, sellQty, targetPrice, "stop loss");
            }
        }

        return new TradeSignal(TradeSignal.Action.HOLD, 0, targetPrice, "no signal");
    }

    @Override
    public void reset() {
        targetDate = "";
        targetPrice = 0;
    }
}
