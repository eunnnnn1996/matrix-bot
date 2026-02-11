package com.example.bithumb.controller;

import com.example.bithumb.repository.TradeHistoryRepository;
import com.example.bithumb.service.DashboardService;
import com.example.bithumb.service.TradeService;
import com.example.dto.TodayProfitDto;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/trade") 
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;
    private final DashboardService dashboardService;
    private final TradeHistoryRepository tradeHistoryRepository;

    // 잔액조회
    @GetMapping("/balance")
    public List<Map<String, Object>> getBalance() {
        return tradeService.getBalance();
    }

    // 오늘 수익
    @GetMapping("/today-profit")
    public TodayProfitDto todayProfit(@RequestParam(defaultValue = "BTC") String coin) {
        return dashboardService.getTodayProfit(coin);
    }

    @GetMapping("/history")
    public Object getAllHistory() {
        return tradeHistoryRepository.findAll();
    }

    @PostMapping("/bot/start") 
    public Map<String, Object> startBot() {
        tradeService.startBot();
        return Map.of("running", true);
    }

    @PostMapping("/bot/stop")
    public Map<String, Object> stopBot() {
        tradeService.stopBot();
        return Map.of("running", false);
    }

    @GetMapping("/bot/status")
    public Map<String, Object> status() {
        return Map.of("running", tradeService.isRunning());
    }
}
