package com.example.bithumb.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.bithumb.domain.TradeLog;
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

        double realizedPnlSum = 0;

        for (TradeLog t : logs) {
            if (!"SELL".equalsIgnoreCase(t.getSide())) continue;

            // 정석: SELL 로그에 realizedPnl을 저장해두고 그걸 합산한다.
            Double pnl = t.getRealizedPnl();
            if (pnl != null) {
                realizedPnlSum += pnl;
            } else {
                // 과거 데이터(컬럼 추가 전) 호환: avgBuyAtTrade가 있으면 그걸로 계산
                Double avg = t.getAvgBuyAtTrade();
                if (avg != null && avg > 0) {
                    realizedPnlSum += (t.getPrice() - avg) * t.getQty();
                }
            }
        }

        return TodayProfitDto.of(coin, realizedPnlSum, logs.size());
    }
}
