package com.example.bithumb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BithumbAutoTradeApplication {
    public static void main(String[] args) {
        SpringApplication.run(BithumbAutoTradeApplication.class, args);
    }
}
