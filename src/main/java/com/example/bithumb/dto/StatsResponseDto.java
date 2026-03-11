package com.example.bithumb.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StatsResponseDto {

    private StatsDto stats;
    private List<StatsTrendDto> trend;

}