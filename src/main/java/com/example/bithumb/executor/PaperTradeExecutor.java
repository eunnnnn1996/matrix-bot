package com.example.bithumb.executor;

import com.example.bithumb.service.BotSettingsService;
import com.example.bithumb.service.BotState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(
        name = "trade.mode",
        havingValue = "paper",
        matchIfMissing = true
)
public class PaperTradeExecutor implements TradeExecutor {

    private final BotSettingsService botSettingsService;
    private final BotState botState;

    private BigDecimal krwBalance;
    private BigDecimal coinBalance = BigDecimal.ZERO;
    private BigDecimal avgBuyPrice = BigDecimal.ZERO;
    private String holdingCoin = null;

    public PaperTradeExecutor(
            @Value("${trade.paper.initialKrw:77996844}") long initialKrw,
            BotSettingsService botSettingsService,
            BotState botState
    ) {
        this.botSettingsService = botSettingsService;
        this.botState = botState;
        this.krwBalance = BigDecimal.valueOf(initialKrw);
    }

    // ===============================
    // BUY
    // ===============================
    @Override
    public synchronized Map<String, Object> buy(
            String coin,
            double price,
            double quantity
    ) {

        if (coinBalance.compareTo(BigDecimal.ZERO) > 0) {
            return Map.of("success", false, "message", "already holding");
        }

        BigDecimal qty = BigDecimal.valueOf(quantity);
        BigDecimal px  = BigDecimal.valueOf(price);
        BigDecimal cost = px.multiply(qty);

        if (krwBalance.compareTo(cost) < 0) {
            return Map.of("success", false, "message", "not enough KRW");
        }

        BigDecimal newCoin = coinBalance.add(qty);

        BigDecimal newAvg = px
                .multiply(qty)
                .divide(newCoin, 8, RoundingMode.HALF_UP);

        krwBalance = krwBalance.subtract(cost);
        coinBalance = newCoin;
        avgBuyPrice = newAvg;
        holdingCoin = coin;

        // 🔥 BotState 동기화
        botState.setHasPosition(true);
        botState.setPositionVolume(coinBalance);
        botState.setPositionAvgBuyPrice(avgBuyPrice.doubleValue());

        return Map.of(
                "success", true,
                "mode", "paper",
                "filled_qty", quantity,
                "avg_price", price
        );
    }

    // ===============================
    // SELL
    // ===============================
    @Override
    public synchronized Map<String, Object> sell(
            String coin,
            double price,
            double quantity
    ) {

        BigDecimal qty = BigDecimal.valueOf(quantity);
        BigDecimal px  = BigDecimal.valueOf(price);

        if (coinBalance.compareTo(qty) < 0) {
            return Map.of("success", false, "message", "not enough coin");
        }

        BigDecimal revenue = px.multiply(qty);

        coinBalance = coinBalance.subtract(qty);
        krwBalance = krwBalance.add(revenue);

        if (coinBalance.compareTo(BigDecimal.ZERO) == 0) {
            avgBuyPrice = BigDecimal.ZERO;
            holdingCoin = null;

            botState.setHasPosition(false);
            botState.setPositionVolume(BigDecimal.ZERO);
            botState.setPositionAvgBuyPrice(null);
        } else {
            botState.setPositionVolume(coinBalance);
        }

        return Map.of(
                "success", true,
                "mode", "paper",
                "filled_qty", quantity,
                "avg_price", price
        );
    }

    // ===============================
    // BALANCE
    // ===============================
    @Override
    public synchronized List<Map<String, Object>> getBalance() {

        String coin = (holdingCoin != null)
                ? holdingCoin
                : botSettingsService.botSelect().getCoin();

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