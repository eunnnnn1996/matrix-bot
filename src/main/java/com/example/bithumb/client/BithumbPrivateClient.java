package com.example.bithumb.client;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BithumbPrivateClient {

    private final RestTemplate restTemplate;

    @Value("${bithumb.api.key}")
    private String accessKey;

    @Value("${bithumb.api.secret}")
    private String secretKey;

    private static final String BASE_URL = "https://api.bithumb.com";

    public List<Map<String, Object>> getBalance() {
        String url = BASE_URL + "/v1/accounts";

        String jwtToken = com.auth0.jwt.JWT.create()
                .withClaim("access_key", accessKey)
                .withClaim("nonce", java.util.UUID.randomUUID().toString())
                .withClaim("timestamp", System.currentTimeMillis())
                .sign(com.auth0.jwt.algorithms.Algorithm.HMAC256(secretKey));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                List.class
        );

        return response.getBody() == null ? List.of() : response.getBody();
    }

     public double getCurrentPrice(String coin) {
        String url = "https://api.bithumb.com/public/ticker/" + coin + "_KRW";
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return 0;

            Object dataObj = response.get("data");
            if (!(dataObj instanceof Map<?, ?> data)) return 0;

            Object closing = data.get("closing_price");
            if (closing == null) return 0;

            return Double.parseDouble(closing.toString());
        } catch (Exception e) {
            return 0;
        }
    }
}
