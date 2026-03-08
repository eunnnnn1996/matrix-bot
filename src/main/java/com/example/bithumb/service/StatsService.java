package com.example.bithumb.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.bithumb.dto.StatsDto;
import com.example.bithumb.repository.StatsRepositoryCustom;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatsService {
    private final StatsRepositoryCustom statsRepository;

    public StatsDto getProfit(String period){

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

        return statsRepository.getStats(from);
    }
}
