package com.example.bithumb.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.bithumb.domain.TradeLog;
import com.example.bithumb.executor.TradeExecutor;
import com.example.bithumb.repository.TradeLogRepository;
import com.example.dto.TodayProfitDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TradeLogRepository tradeLogRepository;

    public TodayProfitDto getTodayProfit(String coin) {

        ZoneId kst = ZoneId.of("Asia/Seoul");
        LocalDateTime start = LocalDate.now(kst).atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        List<TradeLog> logs =
                tradeLogRepository.findByCoinAndCreatedAtBetween(coin, start, end);

        double profit = 0;

        for (TradeLog t : logs) {
            if ("SELL".equalsIgnoreCase(t.getSide())) {
                profit += t.getPrice() * t.getQty();
            }
        }

        return TodayProfitDto.of(coin, profit, logs.size());
    }
}
