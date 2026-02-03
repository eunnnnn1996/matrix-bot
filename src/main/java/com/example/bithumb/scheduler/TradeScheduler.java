package com.example.bithumb.scheduler;

import com.example.bithumb.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradeScheduler {
    private final TradeService tradeService;

    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void run() {
        tradeService.executeAutoTrade("BTC");
    }
}
