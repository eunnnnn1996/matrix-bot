package com.example.bithumb.executor;

import com.example.bithumb.client.BithumbPrivateClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RealTradeExecutor implements TradeExecutor {

    private final BithumbPrivateClient bithumbPrivateClient;

    @Value("${trade.mode:paper}")
    private String tradeMode; // paper | live

    @Value("${trade.live.confirm:false}")
    private boolean liveConfirm;

    @PostConstruct
    public void debugMode() {
        System.out.println("[DEBUG] tradeMode=" + tradeMode + ", liveConfirm=" + liveConfirm);
    }

    @Override
    public List<Map<String, Object>> getBalance() {
        return bithumbPrivateClient.getBalance();
    }

    @Override
    public void buy(String coin, double price, double quantity) {
        String market = "KRW-" + coin;

        if (!"live".equalsIgnoreCase(tradeMode)) {
            System.out.println("[PAPER BUY] " + coin + " price=" + price + " qty=" + quantity);
            return;
        }
        if (!liveConfirm) {
            System.out.println("[BLOCKED] live mode지만 TRADE_LIVE_CONFIRM=false 라서 주문 막음");
            return;
        }

        long krwPrice = Math.round(price);

        // ✅ 과학표기 방지 (1.0E-5 → 0.00001)
        String volumeStr = BigDecimal.valueOf(quantity).stripTrailingZeros().toPlainString();
        String priceStr  = BigDecimal.valueOf(krwPrice).toPlainString();

        Map<String, Object> res = bithumbPrivateClient.placeLimitOrder(
                market,
                "bid",
                volumeStr,
                priceStr
        );

        System.out.println("[LIVE BUY] " + coin + " price=" + krwPrice + " qty=" + volumeStr + " res=" + res);
    }

    @Override
    public void sell(String coin, double price, double quantity) {
        String market = "KRW-" + coin;

        if (!"live".equalsIgnoreCase(tradeMode)) {
            System.out.println("[PAPER SELL] " + coin + " price=" + price + " qty=" + quantity);
            return;
        }
        if (!liveConfirm) {
            System.out.println("[BLOCKED] live mode지만 TRADE_LIVE_CONFIRM=false 라서 주문 막음");
            return;
        }

        long krwPrice = Math.round(price);

        // ✅ 과학표기 방지
        String volumeStr = BigDecimal.valueOf(quantity).stripTrailingZeros().toPlainString();
        String priceStr  = BigDecimal.valueOf(krwPrice).toPlainString();

        Map<String, Object> res = bithumbPrivateClient.placeLimitOrder(
                market,
                "ask",
                volumeStr,
                priceStr
        );

        System.out.println("[LIVE SELL] " + coin + " price=" + krwPrice + " qty=" + volumeStr + " res=" + res);
    }
}
