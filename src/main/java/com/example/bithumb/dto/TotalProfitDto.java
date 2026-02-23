package com.example.bithumb.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TotalProfitDto {

    private String coin;

    // 누적 실현 손익 (SELL 기준 확정 수익)
    private double totalRealizedPnl;

    // 누적 거래 횟수(원하면 SELL만 카운트해도 됨)
    private int tradeCount;

    public static TotalProfitDto of(String coin, double totalRealizedPnl, int tradeCount) {
        return new TotalProfitDto(coin, totalRealizedPnl, tradeCount);
    }
}