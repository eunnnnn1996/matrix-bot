package com.example.bithumb.scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.bithumb.service.TradeService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TradeScheduler {
    private final TradeService tradeService;

    private final AtomicBoolean lock = new AtomicBoolean(false);

    @Scheduled(fixedDelay = 3000)
    public void run() {
        if (!tradeService.isRunning()) return;

        if (!lock.compareAndSet(false, true)) return;
        try {
            tradeService.executeAutoTrade("BTC");
        } finally {
            lock.set(false);
        }
    }
}
