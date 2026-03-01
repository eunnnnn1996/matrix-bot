package com.example.bithumb.service;

import org.springframework.stereotype.Service;

import com.example.bithumb.domain.BotSettings;
import com.example.bithumb.dto.BotSettingsDto;
import com.example.bithumb.repository.BotSettingsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BotSettingsService {
    private final BotSettingsRepository repository;
    private BotSettingsDto cached;

    public BotSettingsDto botSave(BotSettingsDto req) {

        BotSettings setting = repository.findById(1L).orElse(new BotSettings());

        setting.setCoin(req.getCoin());
        setting.setQuantity(req.getQuantity());
        setting.setK(req.getK());
        setting.setTakeProfit(req.getTakeProfit());
        setting.setStopLoss(req.getStopLoss());
        setting.setTickMs(req.getTickMs());

        repository.save(setting);

        cached = toResponse(setting); // 캐시 업데이트

        return cached;
    }

    public BotSettingsDto botSelect() {
        if (cached != null) {
            return cached;
        }
        BotSettings setting = repository.findById(1L).orElse(new BotSettings());
        cached = toResponse(setting);

        return cached;
    }

    private BotSettingsDto toResponse(BotSettings s) {
        BotSettingsDto r = new BotSettingsDto();
        r.setCoin(s.getCoin());
        r.setQuantity(s.getQuantity());
        r.setK(s.getK());
        r.setTakeProfit(s.getTakeProfit());
        r.setStopLoss(s.getStopLoss());
        r.setTickMs(s.getTickMs());
        return r;
    }
}
