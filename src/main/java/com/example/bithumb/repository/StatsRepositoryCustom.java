package com.example.bithumb.repository;

import java.time.LocalDateTime;

import org.springframework.stereotype.Repository;

import com.example.bithumb.domain.QEvent;
import com.example.bithumb.dto.StatsDto;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class  StatsRepositoryCustom {

     private final JPAQueryFactory queryFactory;

    public StatsDto getStats(LocalDateTime from){

        QEvent event = QEvent.event;

        return queryFactory
                .select(Projections.constructor(
                        StatsDto.class,
                        event.count(),
                        event.profit.sum()
                ))
                .from(event)
                .where(event.createdAt.after(from))
                .fetchOne();
    }

}