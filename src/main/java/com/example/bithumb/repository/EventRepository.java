package com.example.bithumb.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bithumb.domain.Event;

public interface EventRepository extends JpaRepository<Event, Long> {
    
}
