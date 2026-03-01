package com.example.bithumb.dto;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class BotSettingsDto {

    private String coin;
    private Double quantity;
    private Double k;
    private Double takeProfit;
    private Double stopLoss;
    private Long tickMs;
}