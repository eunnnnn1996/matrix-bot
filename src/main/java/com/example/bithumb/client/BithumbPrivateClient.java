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
    public Map<String, Object> placeLimitOrder(String market, String side, String volume, String price) {
        // body 값은 String 고정(해시/문자열 동일성 확보)
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("market", market);
        body.put("side", side);         // bid / ask
        body.put("volume", volume);     // "0.00007"
        body.put("price", price);       // "101853000"
        body.put("ord_type", "limit");

        return postPrivateJson("/v1/orders", body);
    }

    // ===== 미체결 주문 조회 (재시작 동기화용) =====
    public List<Map<String, Object>> getOpenOrders(String market) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("market", market);
        params.put("state", "wait");
        params.put("page", "1");
        params.put("limit", "50");
        params.put("order_by", "desc");
        return getPrivateList("/v1/orders", params);
    }

    // ===== 주문 단건 조회 (uuid로 상태 확인) =====
    public Map<String, Object> getOrder(String uuid) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("uuid", uuid);
        return getPrivateMap("/v1/order", params);
    }

    // =========================
    // Private POST (JSON body 있음)
    // =========================
    private Map<String, Object> postPrivateJson(String path, Map<String, Object> body) {
        String url = baseUrl + path;

        String query = toQueryString(body);       // urlencode된 query
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
            String errBody = e.getResponseBodyAsString();
            System.out.println("[HTTP_ERR] status=" + e.getStatusCode() + " body=" + errBody);
            return Map.of("success", false, "message", "status=" + e.getStatusCode() + " body=" + errBody);
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // =========================
    // Private GET (List 응답)
    // =========================
    private List<Map<String, Object>> getPrivateList(String path, Map<String, Object> queryParams) {
        String url = baseUrl + path;

        String query = toQueryString(queryParams);
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

        String fullUrl = url + "?" + query;
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List> response = restTemplate.exchange(fullUrl, HttpMethod.GET, entity, List.class);
            if (response.getBody() == null) return List.of();
            //noinspection unchecked
            return (List<Map<String, Object>>) response.getBody();
        } catch (HttpStatusCodeException e) {
            System.out.println("[HTTP_ERR] status=" + e.getStatusCode() + " body=" + e.getResponseBodyAsString());
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    // =========================
    // Private GET (Map 응답)
    // =========================
    private Map<String, Object> getPrivateMap(String path, Map<String, Object> queryParams) {
        String url = baseUrl + path;

        String query = toQueryString(queryParams);
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

        String fullUrl = url + "?" + query;
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(fullUrl, HttpMethod.GET, entity, Map.class);
            if (response.getBody() == null) return Map.of();
            //noinspection unchecked
            return (Map<String, Object>) response.getBody();
        } catch (HttpStatusCodeException e) {
            System.out.println("[HTTP_ERR] status=" + e.getStatusCode() + " body=" + e.getResponseBodyAsString());
            return Map.of();
        } catch (Exception e) {
            return Map.of();
        }
    }

    // =========================
    // Query string 생성 (정렬 X, insertion order 그대로)
    // =========================
    private String toQueryString(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        int i = 0;

        for (Map.Entry<String, Object> e : params.entrySet()) {
            if (i++ > 0) sb.append("&");
            sb.append(urlEncode(String.valueOf(e.getKey())))
              .append("=")
              .append(urlEncode(String.valueOf(e.getValue())));
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

    // ===== 주문 취소 (uuid) =====
    public Map<String, Object> cancelOrder(String uuid) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("uuid", uuid);
        return postPrivateJson("/v1/order", body); // 빗썸 취소는 DELETE인 경우도 있어 계정/API 스펙에 따라 조정 필요
    }

}
