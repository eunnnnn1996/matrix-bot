package com.example.bithumb.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.bithumb.domain.Event;
import com.example.bithumb.dto.EventDto;
import com.example.bithumb.repository.EventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    // Controller(API)용
    public EventDto eventSave(EventDto dto) {
        Event saved = eventRepository.save(Event.from(dto));
        return EventDto.from(saved);
    }

    // 자동매매 내부 호출용
    public void eventSave(String eventType,
                          String status,
                          String coin,
                          String side,
                          double price,
                          double qty,
                          String message) {

        Event event = Event.builder()
                .eventType(eventType)
                .status(status)
                .coin(coin)
                .side(side)
                .price(price)
                .qty(qty)
                .message(message)
                .build();

        eventRepository.save(event);
    }

    // 🔥 페이징 조회 (거래내역 화면용)
    public Page<EventDto> getEvents(Pageable pageable) {

        return eventRepository.findAll(pageable)
                .map(EventDto::from);
    }
}