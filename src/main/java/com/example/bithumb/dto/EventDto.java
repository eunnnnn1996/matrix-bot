package com.example.bithumb.dto;

import java.time.LocalDateTime;

import com.example.bithumb.domain.Event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventDto {

    private Long id;
    private LocalDateTime createdAt;

    private String eventType;
    private String status;

    private String coin;
    private String side;

    private Double price;
    private Double qty;

    private String message;

    // Entity → DTO 변환
    public static EventDto from(Event e) {
        return EventDto.builder()
                .id(e.getId())
                .createdAt(e.getCreatedAt())
                .eventType(e.getEventType())
                .status(e.getStatus())
                .coin(e.getCoin())
                .side(e.getSide())
                .price(e.getPrice())
                .qty(e.getQty())
                .message(e.getMessage())
                .build();
    }
}