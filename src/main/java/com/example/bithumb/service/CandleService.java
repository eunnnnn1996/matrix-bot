package com.example.bithumb.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CandleService {

    private final RestTemplate restTemplate;
    private static final String BASE_URL = "https://api.bithumb.com";

    // 24h 캔들: /public/candlestick/BTC_KRW/24h
    public List<List<Object>> getCandles24h(String coin) {
        /* String url = BASE_URL + "/public/candlestick/" + coin + "_KRW/24h"; */
        String url = BASE_URL + "/public/candlestick/" + coin + "_KRW/1m";
        Map<String, Object> res = restTemplate.getForObject(url, Map.class);
        if (res == null) return List.of();

        Object dataObj = res.get("data");
        if (!(dataObj instanceof List<?> list)) return List.of();

        // data: [ [time, open, close, high, low, volume], ... ] 형태가 일반적
        // (실제로 한 번 찍어서 인덱스만 확정하면 됨)
        
        @SuppressWarnings("unchecked")
        List<List<Object>> candles = (List<List<Object>>) (List<?>) list;
        // 👇 여기
        if (!candles.isEmpty()) {
            System.out.println("candles sample=" + candles.get(candles.size() - 1));
        }
        return candles;
    }

    public double getTodayOpen(String coin) {
        var candles = getCandles24h(coin);
        if (candles.size() < 1) return 0;

        var latest = candles.get(candles.size() - 1);
        return toDouble(latest, 1); // open
    }

    public double getYesterdayRange(String coin) {
        var candles = getCandles24h(coin);
        if (candles.size() < 2) return 0;

        var prev = candles.get(candles.size() - 2);
        double high = toDouble(prev, 3);
        double low  = toDouble(prev, 4);
        return high - low;
    }

    private double toDouble(List<Object> arr, int idx) {
        if (arr.size() <= idx || arr.get(idx) == null) return 0;
        return Double.parseDouble(arr.get(idx).toString());
    }
}
