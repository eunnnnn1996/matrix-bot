package com.example.bithumb.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class TradeHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 오라클: ID

    private String coin; // 오라클: COIN
    
    // tradeType 필드에 명시적 매핑
    @Column(name = "TRADETYPE") // 데이터베이스 컬럼 이름이 'TRADETYPE'이라고 가정
    private String tradeType; // buy/sell
    
    private double price; // 오라클: PRICE
    
    // tradedAt 필드에 명시적 매핑
    @Column(name = "TRADEDAT") // 데이터베이스 컬럼 이름이 'TRADEDAT'이라고 가정
    private LocalDateTime tradedAt;
}