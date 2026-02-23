package com.example.bithumb.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.bithumb.client.BithumbPrivateClient;
import com.example.bithumb.domain.TradeLog;
import com.example.bithumb.dto.TodayProfitDto;
import com.example.bithumb.dto.TotalAssetsDto;
import com.example.bithumb.dto.TotalProfitDto;
import com.example.bithumb.repository.TradeLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TradeLogRepository tradeLogRepository;
    private final TradeService tradeService;
    private final BithumbPrivateClient bithumbPrivateClient;

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
     // ✅ 추가: 총자산 (BTC만)
    public TotalAssetsDto getTotalAssets() {

        List<Map<String, Object>> balances = tradeService.getBalance();

        long krwBalance = 0L;
        double btcBalance = 0.0;

        for (Map<String, Object> row : balances) {
            String currency = String.valueOf(row.getOrDefault("currency", "")).toUpperCase();

            if ("KRW".equals(currency)) {
                krwBalance = parseLongSafe(row.get("balance"));
            } else if ("BTC".equals(currency)) {
                btcBalance = parseDoubleSafe(row.get("balance"));
            }
        }

        double btcPrice = bithumbPrivateClient.getCurrentPrice("BTC"); // 현재가 (원)
        long btcValueKrw = Math.round(btcBalance * btcPrice);
        long totalAssetsKrw = krwBalance + btcValueKrw;

        return TotalAssetsDto.of(
                "BTC",
                krwBalance,
                btcBalance,
                btcPrice,
                btcValueKrw,
                totalAssetsKrw
        );
    }
    
    public TotalProfitDto getTotalProfit(String coin) {

        List<TradeLog> logs = tradeLogRepository.findByCoin(coin);

        double totalRealizedPnl = 0;
        int sellCount = 0;

        for (TradeLog t : logs) {
            if (!"SELL".equalsIgnoreCase(t.getSide())) continue;
            sellCount++;

            Double pnl = t.getRealizedPnl();
            if (pnl != null) {
                totalRealizedPnl += pnl;
            } else {
                // 과거 데이터(컬럼 추가 전) 호환
                Double avg = t.getAvgBuyAtTrade();
                if (avg != null && avg > 0) {
                    totalRealizedPnl += (t.getPrice() - avg) * t.getQty();
                }
            }
        }

        return TotalProfitDto.of(coin, totalRealizedPnl, sellCount);
    }
    
    private long parseLongSafe(Object v) {
        if (v == null) return 0L;
        try {
            double d = Double.parseDouble(v.toString());
            return (long) Math.floor(d);
        } catch (Exception e) {
            return 0L;
        }
    }

    private double parseDoubleSafe(Object v) {
        if (v == null) return 0.0;
        try {
            return Double.parseDouble(v.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }
}
