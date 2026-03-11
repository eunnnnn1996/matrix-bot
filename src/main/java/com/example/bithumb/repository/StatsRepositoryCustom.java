package com.example.bithumb.repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.example.bithumb.dto.StatsDto;
import com.example.bithumb.dto.StatsTrendDto;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
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
                        ((Number) row[6]).longValue(),
                        ((Number) row[7]).longValue()
                );
        }
        public List<StatsTrendDto> getTrend(LocalDateTime from){

                String sql = """
                        SELECT TRUNC(TE.CREATED_AT) AS created_at,
                        SUM(SUM(TE.PROFIT)) OVER (ORDER BY TRUNC(TE.CREATED_AT)) AS cumulative_profit,
                        SUM(TE.PROFIT) AS profit,
                        COUNT(*) AS cnt
                        FROM TRADE_EVENT TE
                        WHERE TE.CREATED_AT >= :from
                        GROUP BY TRUNC(TE.CREATED_AT)
                        ORDER BY TRUNC(TE.CREATED_AT) ASC
                """;

                Query query = em.createNativeQuery(sql);
                query.setParameter("from", from);

                List<Object[]> rows = query.getResultList();
                List<StatsTrendDto> result = new ArrayList<>();

                for(Object[] row : rows){
                        Timestamp ts = (Timestamp) row[0];

                        result.add(new StatsTrendDto(
                                new Date(ts.getTime()),
                                ((Number) row[1]).longValue(),
                                ((Number) row[2]).longValue(),
                                ((Number) row[3]).longValue()
                        ));
                }

                return result;
                }

}