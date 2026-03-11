package com.example.bithumb.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.bithumb.dto.StatsDto;
import com.example.bithumb.dto.StatsResponseDto;
import com.example.bithumb.dto.StatsTrendDto;
import com.example.bithumb.repository.StatsRepositoryCustom;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsRepositoryCustom statsRepository;

    public StatsResponseDto getProfit(String period){

        LocalDateTime from;

        if(period.equals("1d")){
            from = LocalDateTime.now().minusDays(1);
        }
        else if(period.equals("7d")){
            from = LocalDateTime.now().minusDays(7);
        }
        else if(period.equals("30d")){
            from = LocalDateTime.now().minusDays(30);
        }
        else{
            from = LocalDateTime.of(2000,1,1,0,0);
        }

        StatsDto summary = statsRepository.getTotalProfit(from);
        List<StatsTrendDto> trend = statsRepository.getTrend(from);

        return new StatsResponseDto(summary, trend);
    }
}
