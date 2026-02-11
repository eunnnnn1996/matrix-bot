package com.example.bithumb.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bithumb.domain.TradeLog;

public interface TradeLogRepository extends JpaRepository<TradeLog, Long> {

    List<TradeLog> findByCoinAndCreatedAtBetween(
            String coin,
            LocalDateTime start,
            LocalDateTime end
    );
}

