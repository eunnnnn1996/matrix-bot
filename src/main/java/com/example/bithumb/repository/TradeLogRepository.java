package com.example.bithumb.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bithumb.domain.TradeLog;

public interface TradeLogRepository extends JpaRepository<TradeLog, Long> {
}

