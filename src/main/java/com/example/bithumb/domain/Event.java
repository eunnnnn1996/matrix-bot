package com.example.bithumb.domain;

import java.time.LocalDateTime;

import com.example.bithumb.dto.EventDto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trade_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이벤트 발생 시간
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ORDER / FILLED / ERROR
    @Column(name = "event_type", nullable = false, length = 20)
    private String eventType;

    // SUCCESS / FAIL
    @Column(nullable = false, length = 20)
    private String status;

    // BTC
    @Column(length = 10)
    private String coin;

    // BUY / SELL
    @Column(length = 10)
    private String side;

    // 가격
    @Column
    private Double price;

    // 수량
    @Column
    private Double qty;

    // 발송용 메시지 (핵심)
    @Column(length = 300)
    private String message;

    // 생성 편의 메서드 (실무에서 자주 씀)
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ⭐ DTO → Entity 변환
    public static Event from(EventDto dto) {
        return Event.builder()
                .eventType(dto.getEventType())
                .status(dto.getStatus())
                .coin(dto.getCoin())
                .side(dto.getSide())
                .price(dto.getPrice())
                .qty(dto.getQty())
                .message(dto.getMessage())
                .build();
    }
}
