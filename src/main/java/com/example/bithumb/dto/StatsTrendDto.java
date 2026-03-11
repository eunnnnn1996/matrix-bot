package com.example.bithumb.dto;

import java.sql.Date;

import lombok.Getter;

@Getter
public class StatsTrendDto {

    private Date created_at;
    private Long cumulative_profit;
    private Long profit;
    private Long cnt;

    public StatsTrendDto(Date created_at, Long cumulative_profit, Long profit, Long cnt) {
        this.created_at = created_at;
        this.cumulative_profit = cumulative_profit;
        this.profit = profit;
        this.cnt = cnt;
    }
}
