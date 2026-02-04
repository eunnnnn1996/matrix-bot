package com.example.bithumb.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.bithumb.service.TradeService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TradeScheduler {
    private final TradeService tradeService;

    @Scheduled(fixedRate = 3000)
    public void run() {
        if (!tradeService.isRunning()) return; 
        tradeService.executeAutoTrade("BTC");
    }
}