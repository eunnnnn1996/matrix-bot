package com.example.bithumb.executor;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.bithumb.client.BithumbPrivateClient;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RealTradeExecutor implements TradeExecutor {

    private final BithumbPrivateClient bithumbPrivateClient;

    @Override
    public List<Map<String, Object>> getBalance() {
        return bithumbPrivateClient.getBalance();
    }

    @Override
    public void buy(String coin, double price, double quantity) {
        System.out.println("[BUY] " + coin + " price=" + price + " qty=" + quantity);
    }
    @Override
    public void sell(String coin, double price, double quantity) {
        System.out.println("[SELL] " + coin + " price=" + price + " qty=" + quantity);
    }

}

