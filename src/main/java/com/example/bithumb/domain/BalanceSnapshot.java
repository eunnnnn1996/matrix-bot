package com.example.bithumb.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "BALANCE_SNAPSHOT")
@Getter
@NoArgsConstructor
public class BalanceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String exchange = "BITHUMB";

    @Column(nullable = false)
    private String coin;          // BTC

    @Column(name = "KRW_BAL")
    private Long krwBal;          // 원화 잔고(정수)

    @Column(name = "COIN_BAL")
    private Double coinBal;       // 코인 보유수량

    @Column(name = "AVG_BUY")
    private Long avgBuy;          // 평균매수가(원화 정수)

    @Column(name = "CURRENT_PX")
    private Long currentPx;       // 스냅샷 시점 현재가(원화 정수)

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt = LocalDateTime.now();

    public BalanceSnapshot(String coin, Long krwBal, Double coinBal, Long avgBuy, Long currentPx) {
        this.coin = coin;
        this.krwBal = krwBal;
        this.coinBal = coinBal;
        this.avgBuy = avgBuy;
        this.currentPx = currentPx;
    }
}
