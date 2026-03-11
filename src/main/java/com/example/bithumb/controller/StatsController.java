package com.example.bithumb.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bithumb.dto.StatsResponseDto;
import com.example.bithumb.service.StatsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/getProfit")
    public StatsResponseDto getProfit(String period) {
        return statsService.getProfit(period);
    }
}
