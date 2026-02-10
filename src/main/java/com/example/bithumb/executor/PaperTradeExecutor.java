package com.example.bithumb.executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "trade.mode", havingValue = "paper", matchIfMissing = true)
public class PaperTradeExecutor implements TradeExecutor {

    private final String coin = "BTC"; // 현재 프로젝트 기본이 BTC라 고정(확장하려면 맵으로 바꾸면 됨)

    private BigDecimal krwBalance;
    private BigDecimal coinBalance = BigDecimal.ZERO;
    private BigDecimal avgBuyPrice = BigDecimal.ZERO; // KRW 기준

    public PaperTradeExecutor(@Value("${trade.paper.initialKrw:77996844}") long initialKrw) {
        this.krwBalance = BigDecimal.valueOf(initialKrw);
        System.out.println("[PAPER] initialKrw=" + initialKrw);
    }

    @Override
    public synchronized Map<String, Object> buy(String coin, double price, double quantity) {
        BigDecimal qty = BigDecimal.valueOf(quantity);
        BigDecimal px  = BigDecimal.valueOf(price);

        BigDecimal cost = px.multiply(qty);

        if (krwBalance.compareTo(cost) < 0) {
            System.out.println("[PAPER BUY FAIL] not enough KRW. need=" + cost + " have=" + krwBalance);
            return Map.of("success", false, "mode", "paper", "message", "not enough KRW");
        }

        // 평단(가중 평균) 업데이트
        BigDecimal prevValue = avgBuyPrice.multiply(coinBalance);
        BigDecimal addValue  = px.multiply(qty);
        BigDecimal newCoin   = coinBalance.add(qty);

        BigDecimal newAvg = BigDecimal.ZERO;
        if (newCoin.compareTo(BigDecimal.ZERO) > 0) {
            newAvg = prevValue.add(addValue)
                    .divide(newCoin, 0, RoundingMode.HALF_UP); // 원화 단위
        }

        krwBalance = krwBalance.subtract(cost);
        coinBalance = newCoin;
        avgBuyPrice = newAvg;

        System.out.println("[PAPER BUY] " + coin + " price=" + price + " qty=" + quantity
                + " krw=" + krwBalance + " " + coin + "=" + coinBalance + " avgBuy=" + avgBuyPrice);

        return Map.of(
                "success", true,
                "mode", "paper",
                "filled_qty", quantity,
                "avg_price", price
        );
    }

    @Override
    public synchronized Map<String, Object> sell(String coin, double price, double quantity) {
        BigDecimal qty = BigDecimal.valueOf(quantity);
        BigDecimal px  = BigDecimal.valueOf(price);

        if (coinBalance.compareTo(qty) < 0) {
            System.out.println("[PAPER SELL FAIL] not enough " + coin + ". sell=" + qty + " have=" + coinBalance);
            return Map.of("success", false, "mode", "paper", "message", "not enough coin");
        }

        BigDecimal revenue = px.multiply(qty);

        coinBalance = coinBalance.subtract(qty);
        krwBalance = krwBalance.add(revenue);

        if (coinBalance.compareTo(BigDecimal.ZERO) == 0) {
            avgBuyPrice = BigDecimal.ZERO;
        }

        System.out.println("[PAPER SELL] " + coin + " price=" + price + " qty=" + quantity
                + " krw=" + krwBalance + " " + coin + "=" + coinBalance + " avgBuy=" + avgBuyPrice);

        return Map.of(
                "success", true,
                "mode", "paper",
                "filled_qty", quantity,
                "avg_price", price
        );
    }

    @Override
    public synchronized List<Map<String, Object>> getBalance() {
        // Bithumb /v1/accounts 형태 흉내: currency, balance, locked, avg_buy_price
        Map<String, Object> krw = Map.of(
                "currency", "KRW",
                "balance", krwBalance.stripTrailingZeros().toPlainString(),
                "locked", "0",
                "avg_buy_price", "0",
                "unit_currency", "KRW"
        );

        Map<String, Object> c = Map.of(
                "currency", this.coin,
                "balance", coinBalance.stripTrailingZeros().toPlainString(),
                "locked", "0",
                "avg_buy_price", avgBuyPrice.stripTrailingZeros().toPlainString(),
                "unit_currency", "KRW"
        );

        return List.of(krw, c);
    }
}
