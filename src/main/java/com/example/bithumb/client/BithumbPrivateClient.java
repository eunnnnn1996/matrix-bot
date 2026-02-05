package com.example.bithumb.client;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Component
@RequiredArgsConstructor
public class BithumbPrivateClient {

    private final RestTemplate restTemplate;

    @Value("${bithumb.api.key}")
    private String accessKey;

    @Value("${bithumb.api.secret}")
    private String secretKey;

    @Value("${bithumb.api.url:https://api.bithumb.com}")
    private String baseUrl;

    // ===== 잔고 =====
    public List<Map<String, Object>> getBalance() {
        String url = baseUrl + "/v1/accounts";

        String jwtToken = JWT.create()
                .withClaim("access_key", accessKey)
                .withClaim("nonce", UUID.randomUUID().toString())
                .withClaim("timestamp", System.currentTimeMillis())
                .sign(Algorithm.HMAC256(secretKey));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
        if (response.getBody() == null) return List.of();
        //noinspection unchecked
        return (List<Map<String, Object>>) response.getBody();
    }

    // ===== 현재가(퍼블릭) =====
    public double getCurrentPrice(String coin) {
        String url = baseUrl + "/public/ticker/" + coin + "_KRW";
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

    // ===== 주문(지정가) =====
    // market: "KRW-BTC" 형태로 들어가는게 일반적. (네 코드 흐름은 KRW-COIN 사용중)
    public Map<String, Object> placeLimitOrder(String market, String side, String volume, String price) {
        // ✅ body 값은 무조건 String으로 고정 (query_hash와 100% 동일 문자열 보장)
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("market", market);
        body.put("side", side);         // bid(매수) / ask(매도)
        body.put("volume", volume);     // "0.00001" (과학표기 금지)
        body.put("price", price);       // "102119000"
        body.put("ord_type", "limit");

        return postPrivate("/v1/orders", body);
    }

    private Map<String, Object> postPrivate(String path, Map<String, Object> body) {
        String url = baseUrl + path;

        String query = toSortedQueryString(body);
        String queryHash = sha512Hex(query);

        String jwtToken = JWT.create()
                .withClaim("access_key", accessKey)
                .withClaim("nonce", UUID.randomUUID().toString())
                .withClaim("timestamp", System.currentTimeMillis())
                .withClaim("query_hash", queryHash)
                .withClaim("query_hash_alg", "SHA512")
                .sign(Algorithm.HMAC256(secretKey));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            if (response.getBody() == null) return Map.of("success", false, "message", "empty response");
            //noinspection unchecked
            return (Map<String, Object>) response.getBody();
        } catch (HttpStatusCodeException e) {
            // ✅ 여기서 진짜 원인 코드가 나온다
            String errBody = e.getResponseBodyAsString();
            System.out.println("[HTTP_ERR] status=" + e.getStatusCode() + " body=" + errBody);
            return Map.of("success", false, "message", "status=" + e.getStatusCode() + " body=" + errBody);
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // ✅ 핵심: 키 정렬 + URL 인코딩 + "key=value&..." 문자열 생성
    private String toSortedQueryString(Map<String, Object> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String k = keys.get(i);
            Object v = params.get(k);

            if (i > 0) sb.append("&");
            sb.append(urlEncode(k)).append("=").append(urlEncode(String.valueOf(v)));
        }
        return sb.toString();
    }

    private String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private String sha512Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
