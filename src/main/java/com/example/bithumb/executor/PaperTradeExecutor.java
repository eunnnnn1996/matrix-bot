package com.example.bithumb.executor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.example.bithumb.service.BotSettingsService;

import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnProperty(
        name = "trade.mode",
        havingValue = "paper",
        matchIfMissing = true
)
public class PaperTradeExecutor implements TradeExecutor {

    private final BotSettingsService botSettingsService;

    private BigDecimal krwBalance;
    private BigDecimal coinBalance = BigDecimal.ZERO;
    private BigDecimal avgBuyPrice = BigDecimal.ZERO;

    public PaperTradeExecutor(
            @Value("${trade.paper.initialKrw:77996844}") long initialKrw,
            BotSettingsService botSettingsService
    ) {
        this.botSettingsService = botSettingsService;
        this.krwBalance = BigDecimal.valueOf(initialKrw);

        System.out.println("[PAPER] initialKrw=" + initialKrw);
    }
    // ===============================
    // helper
    // ===============================
    private String getCoin() {
        return botSettingsService.botSelect().getCoin();
    }

    @Override
    public synchronized Map<String, Object> buy(
            String coin,
            double price,
            double quantity
    ) {

        BigDecimal qty = BigDecimal.valueOf(quantity);
        BigDecimal px  = BigDecimal.valueOf(price);

        BigDecimal cost = px.multiply(qty);

        if (krwBalance.compareTo(cost) < 0) {
            System.out.println("[PAPER BUY FAIL] not enough KRW. need="
                    + cost + " have=" + krwBalance);

            return Map.of(
                    "success", false,
                    "mode", "paper",
                    "message", "not enough KRW"
            );
        }

        // 평단 계산
        BigDecimal prevValue = avgBuyPrice.multiply(coinBalance);
        BigDecimal addValue  = px.multiply(qty);
        BigDecimal newCoin   = coinBalance.add(qty);

        BigDecimal newAvg = BigDecimal.ZERO;
        if (newCoin.compareTo(BigDecimal.ZERO) > 0) {
            newAvg = prevValue.add(addValue)
                    .divide(newCoin, 0, RoundingMode.HALF_UP);
        }

        krwBalance = krwBalance.subtract(cost);
        coinBalance = newCoin;
        avgBuyPrice = newAvg;

        System.out.println("[PAPER BUY] " + coin
                + " price=" + price
                + " qty=" + quantity
                + " krw=" + krwBalance
                + " " + coin + "=" + coinBalance
                + " avgBuy=" + avgBuyPrice);

        return Map.of(
                "success", true,
                "mode", "paper",
                "filled_qty", quantity,
                "avg_price", price
        );
    }

    @Override
    public synchronized Map<String, Object> sell(
            String coin,
            double price,
            double quantity
    ) {

        BigDecimal qty = BigDecimal.valueOf(quantity);
        BigDecimal px  = BigDecimal.valueOf(price);

        if (coinBalance.compareTo(qty) < 0) {
            System.out.println("[PAPER SELL FAIL] not enough " + coin
                    + ". sell=" + qty + " have=" + coinBalance);

            return Map.of(
                    "success", false,
                    "mode", "paper",
                    "message", "not enough coin"
            );
        }

        BigDecimal revenue = px.multiply(qty);

        coinBalance = coinBalance.subtract(qty);
        krwBalance = krwBalance.add(revenue);

        if (coinBalance.compareTo(BigDecimal.ZERO) == 0) {
            avgBuyPrice = BigDecimal.ZERO;
        }

        System.out.println("[PAPER SELL] " + coin
                + " price=" + price
                + " qty=" + quantity
                + " krw=" + krwBalance
                + " " + coin + "=" + coinBalance
                + " avgBuy=" + avgBuyPrice);

        return Map.of(
                "success", true,
                "mode", "paper",
                "filled_qty", quantity,
                "avg_price", price
        );
    }

    @Override
    public synchronized List<Map<String, Object>> getBalance() {

        String coin = getCoin(); // ⭐ settings에서 가져옴

        Map<String, Object> krw = Map.of(
                "currency", "KRW",
                "balance", krwBalance.stripTrailingZeros().toPlainString(),
                "locked", "0",
                "avg_buy_price", "0",
                "unit_currency", "KRW"
        );

        Map<String, Object> c = Map.of(
                "currency", coin,
                "balance", coinBalance.stripTrailingZeros().toPlainString(),
                "locked", "0",
                "avg_buy_price", avgBuyPrice.stripTrailingZeros().toPlainString(),
                "unit_currency", "KRW"
        );

        return List.of(krw, c);
    }
}