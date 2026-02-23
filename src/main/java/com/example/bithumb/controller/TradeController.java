package com.example.bithumb.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bithumb.dto.TodayProfitDto;
import com.example.bithumb.dto.TotalAssetsDto;
import com.example.bithumb.dto.TotalProfitDto;
import com.example.bithumb.repository.TradeHistoryRepository;
import com.example.bithumb.service.DashboardService;
import com.example.bithumb.service.TradeService;

import lombok.RequiredArgsConstructor;

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

    // 총 자산
    @GetMapping("/total-assets")
    public TotalAssetsDto totalAssets() {
        return dashboardService.getTotalAssets();
    }

    // 총 수익(누적 실현손익)
    @GetMapping("/total-profit")
    public TotalProfitDto totalProfit(@RequestParam(defaultValue = "BTC") String coin) {
        return dashboardService.getTotalProfit(coin);
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
