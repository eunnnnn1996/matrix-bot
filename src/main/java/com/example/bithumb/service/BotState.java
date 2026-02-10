package com.example.bithumb.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
@Getter
@Setter
public class BotState {
    // 포지션(보유) 상태
    private boolean hasPosition = false;
    private BigDecimal positionVolume = BigDecimal.ZERO;
    private Double positionAvgBuyPrice = null;

    // 미체결 주문 상태
    private boolean hasOpenOrder = false;
    private String openOrderUuid = null;
    private String openOrderSide = null; // bid / ask

    // ✅ 오픈오더 생성 시각(자동취소용)
    private Instant openOrderCreatedAt = null;

    // 안전장치(쿨다운)
    private Instant lastBuyAt = null;
    private Instant lastSellAt = null;
}
