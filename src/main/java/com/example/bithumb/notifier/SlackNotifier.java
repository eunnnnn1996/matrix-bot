package com.example.bithumb.notifier;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SlackNotifier {

    private final RestTemplate restTemplate;

    @Value("${slack.webhookUrl:}")
    private String webhookUrl;

    public void send(String message) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            System.out.println("[SLACK] webhookUrl is empty. skip. msg=" + message);
            return;
        }

        // Slack Incoming Webhook 기본 payload
        restTemplate.postForObject(webhookUrl, Map.of("text", message), String.class);
    }
}