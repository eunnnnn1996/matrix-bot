package com.example.bithumb.repository;

import jakarta.persistence.Query;

import java.time.LocalDateTime;

import org.springframework.stereotype.Repository;

import com.example.bithumb.dto.StatsDto;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class  StatsRepositoryCustom {

        private final EntityManager em;

    public StatsDto getTotalProfit(LocalDateTime from){

        StringBuilder sql = new StringBuilder();

        sql.append("""
                SELECT
                NVL(SUM(TE.PROFIT),0) AS totalProfit,
                NVL(ROUND(
                        SUM(CASE WHEN TE.PROFIT > 0 THEN 1 ELSE 0 END) * 100.0 /
                        NULLIF(SUM(CASE WHEN TE.PROFIT != 0 THEN 1 ELSE 0 END),0)
                ,1),0) AS winRate,
                NVL(COUNT(TE.PROFIT),0) AS totalTrade,
                NVL(ROUND(AVG(TE.PROFIT),0),0) AS avgProfit,
                NVL(MAX(TE.PROFIT),0) AS maxProfit,
                NVL(MIN(TE.PROFIT),0) AS minLoss,
                NVL(SUM(CASE WHEN TE.PROFIT > 0 THEN 1 ELSE 0 END),0) AS profitCnt,
                NVL(SUM(CASE WHEN TE.PROFIT < 0 THEN 1 ELSE 0 END),0) AS lossCnt
                FROM TRADE_EVENT TE
                WHERE TE.CREATED_AT >= :from
        """);

        Query query = em.createNativeQuery(sql.toString());
        query.setParameter("from", from);

        Object[] row = (Object[]) query.getSingleResult();

        return new StatsDto(
                ((Number) row[0]).longValue(),
                ((Number) row[1]).doubleValue(),
                ((Number) row[2]).longValue(),
                ((Number) row[3]).longValue(),
                ((Number) row[4]).longValue(),
                ((Number) row[5]).longValue(),
                ((Number) row[6]).longValue()
        );
    }

}