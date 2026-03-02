package com.example.bithumb.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bithumb.domain.TradeLog;

public interface TradeLogRepository extends JpaRepository<TradeLog, Long> {

    List<TradeLog> findByCoinAndCreatedAtBetween(
            String coin,
            LocalDateTime start,
            LocalDateTime end
    );

    List<TradeLog> findByCoin(String coin);
    // 최신 로그
    Optional<TradeLog> findTopByCoinOrderByIdDesc(String coin);

    // 시작 이후 마지막 ID 값
    List<TradeLog> findTop50ByCoinAndIdGreaterThanOrderByIdAsc(String coin, Long id);
    
    Optional<TradeLog> findTopByCoinAndSideOrderByIdDesc(String coin, String side);

    Optional<TradeLog> findTopByCoinOrderByCreatedAtDesc(String coin);
}

