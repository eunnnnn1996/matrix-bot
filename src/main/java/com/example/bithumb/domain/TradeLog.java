package com.example.bithumb.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "TRADE_LOG")
@Getter
@NoArgsConstructor
public class TradeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String coin;   // BTC

    @Column(nullable = false)
    private String side;   // BUY / SELL

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private double qty;

    private String strategy; // VolatilityBreakout
    private String reason;   // breakout / take_profit / stop_loss

    /**
     * SELL 체결 시점의 평단(원가). BUY일 때는 null이어도 됨.
     * 오늘 실현손익(todayRealizedPnl)을 정확하게 계산하려고 저장.
     */
    @Column(name = "AVG_BUY_AT_TRADE")
    private Double avgBuyAtTrade;

    /**
     * SELL 체결로 확정된 손익(원화). BUY일 때는 0 또는 null.
     */
    @Column(name = "REALIZED_PNL")
    private Double realizedPnl;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt = LocalDateTime.now();

    public TradeLog(String coin, String side, double price, double qty,
                    String strategy, String reason) {
        this.coin = coin;
        this.side = side;
        this.price = price;
        this.qty = qty;
        this.strategy = strategy;
        this.reason = reason;
    }

    // SELL 저장할 때만 쓰는 편의 메서드
    public void setAvgBuyAtTrade(Double avgBuyAtTrade) {
        this.avgBuyAtTrade = avgBuyAtTrade;
    }

    public void setRealizedPnl(Double realizedPnl) {
        this.realizedPnl = realizedPnl;
    }
}
