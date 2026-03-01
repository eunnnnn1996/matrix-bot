package com.example.bithumb.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bot_settings")
@Getter @Setter
public class BotSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String coin;
    private Double quantity;
    private Double k;

    @Column(name="take_profit")
    private Double takeProfit;

    @Column(name="stop_loss")
    private Double stopLoss;

    @Column(name="tick_ms")
    private Long tickMs;

    private LocalDateTime updatedAt;
}