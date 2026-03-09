package com.example.bithumb.dto;

import lombok.Getter;

@Getter
public class StatsDto {

    private Long totalProfit;
    private Double winRate;
    private Long totalTrade;
    private Long avgProfit;
    private Long maxProfit;
    private Long minLoss;
    private Long profitCnt;

    public StatsDto(Long totalProfit, Double winRate, Long totalTrade, Long avgProfit, Long maxProfit, Long minLoss, Long profitCnt) {
        this.totalProfit = totalProfit;
        this.winRate = winRate;
        this.totalTrade = totalTrade;
        this.avgProfit = avgProfit;
        this.maxProfit = maxProfit;
        this.minLoss = minLoss;
        this.profitCnt = profitCnt;
    }
}
