package com.example.telegram_bot.scheduler;

import java.time.LocalDateTime;

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

            System.out.println("========================================");
            System.out.println("Scheduler Started");
            System.out.println("Time : " + LocalDateTime.now());
            System.out.println("Checking Google Sheet for NEW deals...");
            System.out.println("========================================");

            // Process ONLY ONE deal
            googleSheetService.processNextDeal();

            System.out.println("========================================");
            System.out.println("Scheduler Finished");
            System.out.println("Waiting for next schedule...");
            System.out.println("========================================");

        } catch (Exception e) {

            System.out.println("Scheduler Error: " + e.getMessage());

            e.printStackTrace();
        }
    }
}
