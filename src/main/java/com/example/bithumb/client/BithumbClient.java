package com.example.bithumb.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BithumbClient {

    private final RestTemplate restTemplate;

    @Value("${bithumb.api.url}")
    private String apiUrl;

    public double getCurrentPrice(String coin) {
        String url = "https://api.bithumb.com/public/ticker/" + coin + "_KRW";
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            return Double.parseDouble(data.get("closing_price").toString());
        } catch (Exception e) {
            return 0;
        }
    }

    public List<Map<String, Object>> getBalance() {
        return List.of(
            Map.of("currency", "KRW", "balance", "1000000"),
            Map.of("currency", "BTC", "balance", "0.01")
        );
    }
}
