package com.example.bithumb.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TotalAssetsDto {

    private String coin;          // "BTC"
    private long krwBalance;      // KRW 잔액 (원)
    private double btcBalance;    // BTC 보유수량
    private double btcPrice;      // BTC 현재가 (원)
    private long btcValueKrw;     // BTC 평가금액 (원)
    private long totalAssetsKrw;  // 총자산 (원)

    public static TotalAssetsDto of(
            String coin,
            long krwBalance,
            double btcBalance,
            double btcPrice,
            long btcValueKrw,
            long totalAssetsKrw
    ) {
        return new TotalAssetsDto(coin, krwBalance, btcBalance, btcPrice, btcValueKrw, totalAssetsKrw);
    }
}