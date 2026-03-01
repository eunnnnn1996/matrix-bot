package com.example.bithumb.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bithumb.domain.BotSettings;

public interface BotSettingsRepository extends JpaRepository<BotSettings, Long> {
}