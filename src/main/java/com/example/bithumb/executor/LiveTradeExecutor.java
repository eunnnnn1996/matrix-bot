package com.example.bithumb.executor;

import com.example.bithumb.client.BithumbPrivateClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "trade.mode", havingValue = "live")
public class LiveTradeExecutor implements TradeExecutor {

    private final BithumbPrivateClient bithumbPrivateClient;

    @Value("${trade.live.confirm:false}")
    private boolean liveConfirm;

    @PostConstruct
    public void debugMode() {
        System.out.println("[DEBUG] tradeMode=live, liveConfirm=" + liveConfirm);
    }

    @Override
    public List<Map<String, Object>> getBalance() {
        return bithumbPrivateClient.getBalance();
    }

    @Override
    public Map<String, Object> buy(String coin, double price, double quantity) {
        String market = "KRW-" + coin;

        if (!liveConfirm) {
            System.out.println("[BLOCKED] live mode지만 TRADE_LIVE_CONFIRM=false 라서 주문 막음");
            return Map.of("success", false, "message", "blocked: liveConfirm=false");
        }

        long krwPrice = Math.round(price);

        String volumeStr = BigDecimal.valueOf(quantity).stripTrailingZeros().toPlainString();
        String priceStr  = BigDecimal.valueOf(krwPrice).toPlainString();

        Map<String, Object> res = bithumbPrivateClient.placeLimitOrder(
                market, "bid", volumeStr, priceStr
        );

        System.out.println("[LIVE BUY] " + coin + " price=" + krwPrice + " qty=" + volumeStr + " res=" + res);
        return res;
    }

    @Override
    public Map<String, Object> sell(String coin, double price, double quantity) {
        String market = "KRW-" + coin;

        if (!liveConfirm) {
            System.out.println("[BLOCKED] live mode지만 TRADE_LIVE_CONFIRM=false 라서 주문 막음");
            return Map.of("success", false, "message", "blocked: liveConfirm=false");
        }

        long krwPrice = Math.round(price);

        String volumeStr = BigDecimal.valueOf(quantity).stripTrailingZeros().toPlainString();
        String priceStr  = BigDecimal.valueOf(krwPrice).toPlainString();

        Map<String, Object> res = bithumbPrivateClient.placeLimitOrder(
                market, "ask", volumeStr, priceStr
        );

        System.out.println("[LIVE SELL] " + coin + " price=" + krwPrice + " qty=" + volumeStr + " res=" + res);
        return res;
    }
}
