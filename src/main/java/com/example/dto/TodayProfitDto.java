package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TodayProfitDto {

    private String coin;

    // 오늘 실현 손익 (SELL 기준 확정 수익)
    private double todayRealizedPnl;

    // 오늘 거래 횟수
    private int tradeCount;

    public static TodayProfitDto of(String coin, double todayRealizedPnl, int tradeCount) {
        return new TodayProfitDto(coin, todayRealizedPnl, tradeCount);
    }
}
