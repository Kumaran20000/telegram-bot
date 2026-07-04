package com.example.telegram_bot.scheduler;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.telegram_bot.service.GoogleSheetService;

@Component
public class PostScheduler {

    private final GoogleSheetService googleSheetService;

    public PostScheduler(GoogleSheetService googleSheetService) {
        this.googleSheetService = googleSheetService;
    }

    @Scheduled(fixedRate = 300000)
    public void run() {

        try {
            googleSheetService.processSheet();
        } catch (Exception e) {
            System.out.println("Scheduler error: " + e.getMessage());
        }
    }
}