package com.example.bithumb.dto;

import lombok.Getter;

@Getter
public class StatsDto {

    private Long count;
    private Double totalProfit;

    public StatsDto(Long count, Double totalProfit) {
        this.count = count;
        this.totalProfit = totalProfit;
    }

}
