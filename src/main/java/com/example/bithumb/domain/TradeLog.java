package com.example.bithumb.domain;

import jakarta.persistence.*;
import lombok.*;

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
}
