package com.example.bithumb.strategy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.example.bithumb.service.CandleService;
import com.example.bithumb.dto.BotSettingsDto;
import com.example.bithumb.service.BotSettingsService;
import com.example.bithumb.service.BotState;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VolatilityBreakoutStrategy implements TradeStrategy {

    private final CandleService candleService;
    private final BotState botState;
    private final BotSettingsService botSettingsService;

    private String targetDate = "";
    private double targetPrice = 0;

    @Override
    public TradeSignal decide(String coin, double currentPrice) {

        BotSettingsDto settings = botSettingsService.botSelect();

        double k = settings.getK();
        double takeProfit = settings.getTakeProfit();
        double stopLoss = settings.getStopLoss();
        double quantity = settings.getQuantity();

        // 가격 방어
        if (currentPrice <= 0) {
            return new TradeSignal(
                    TradeSignal.Action.HOLD,
                    0,
                    targetPrice,
                    "invalid price"
            );
        }

        // TP/SL 방어
        if (takeProfit <= 0 || stopLoss <= 0) {
            return new TradeSignal(
                    TradeSignal.Action.HOLD,
                    0,
                    targetPrice,
                    "invalid tp/sl"
            );
        }

        String today = LocalDate.now(ZoneId.of("Asia/Seoul")).toString();

        // 날짜 바뀌면 목표가 재계산
        if (!today.equals(targetDate)) {

            double open = candleService.getTodayOpen(coin);
            double range = candleService.getYesterdayRange(coin);

            targetPrice = open + range * k;
            targetDate = today;

            return new TradeSignal(
                    TradeSignal.Action.HOLD,
                    0,
                    targetPrice,
                    "target updated"
            );
        }

        boolean hasPosition = botState.isHasPosition();
        Double avgBuy = botState.getPositionAvgBuyPrice();  // Double 그대로 사용
        BigDecimal posVolumeObj = botState.getPositionVolume();

        double posQty = (posVolumeObj == null)
                ? 0.0
                : posVolumeObj.doubleValue();

        // ==============================
        // 1️⃣ SELL 먼저 판단
        // ==============================
        if (hasPosition && avgBuy != null && avgBuy > 0 && posQty > 0) {

            double tpPrice = avgBuy * (1 + takeProfit);
            double slPrice = avgBuy * (1 - stopLoss);

            if (currentPrice >= tpPrice) {
                return new TradeSignal(
                        TradeSignal.Action.SELL,
                        posQty,
                        avgBuy,
                        "take profit"
                );
            }

            if (currentPrice <= slPrice) {
                return new TradeSignal(
                        TradeSignal.Action.SELL,
                        posQty,
                        avgBuy,
                        "stop loss"
                );
            }
        }

        // ==============================
        // 2️⃣ BUY 판단
        // ==============================
        if (!hasPosition && currentPrice >= targetPrice) {

            return new TradeSignal(
                    TradeSignal.Action.BUY,
                    quantity,
                    targetPrice,
                    "breakout"
            );
        }

        // ==============================
        // 3️⃣ HOLD
        // ==============================
        return new TradeSignal(
                TradeSignal.Action.HOLD,
                0,
                targetPrice,
                "no signal"
        );
    }

    @Override
    public void reset() {
        targetDate = "";
        targetPrice = 0;
    }
}