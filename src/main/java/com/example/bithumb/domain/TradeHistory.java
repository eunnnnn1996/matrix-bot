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
    private Long id;

    private String coin;
    
    @Column(name = "TRADETYPE")
    private String tradeType; 
    
    private double price;
    
    @Column(name = "TRADEDAT")
    private LocalDateTime tradedAt;
}