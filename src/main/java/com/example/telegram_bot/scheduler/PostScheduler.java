package com.example.telegram_bot.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.telegram_bot.service.AmazonSiteStripeService;
import com.example.telegram_bot.service.GoogleSheetService;
import com.example.telegram_bot.service.TelegramService;

@Component
public class PostScheduler {

    private final GoogleSheetService googleSheetService;
    private final AmazonSiteStripeService amazonSiteStripeService;
    private final TelegramService telegramService;

    public PostScheduler(GoogleSheetService googleSheetService, AmazonSiteStripeService amazonSiteStripeService, TelegramService telegramService) {
        this.googleSheetService = googleSheetService;
        this.amazonSiteStripeService = amazonSiteStripeService;
        this.telegramService = telegramService;
    }

     @Scheduled(fixedRate = 300000)
    public void run() {

        try {

            System.out.println("========================================");
            System.out.println("Scheduler Started");
            System.out.println("Time : " + LocalDateTime.now());
            System.out.println("1. Checking Google Sheet for incomplete deals to auto-enrich...");

            try {
                int enrichedCount = amazonSiteStripeService.enrichSheetDeals();
                if (enrichedCount > 0) {
                    System.out.println("✅ Successfully auto-enriched " + enrichedCount + " deal(s) from Amazon.");
                }
            } catch (Exception e) {
                System.err.println("⚠️ Warning during sheet enrichment: " + e.getMessage());
            }

            System.out.println("2. Processing NEW deal for social media posting...");
            System.out.println("========================================");

            // Process ONLY ONE deal
            googleSheetService.processNextDeal();

            System.out.println("========================================");
            System.out.println("Scheduler Finished");
            System.out.println("Waiting for next schedule...");
            System.out.println("========================================");

        } catch (Exception e) {
            if (isNetworkOrDnsError(e)) {
                System.err.println("⚠️ Scheduler Network Warning: Unable to reach Google Auth / API server (" + e.getMessage() + "). Check internet connectivity or DNS configuration. Will retry automatically on next run.");
                telegramService.sendAdminNotification(
                        "🔴 <b>Network Connectivity Alert</b>\n\n" +
                        "⚠️ Unable to reach Google API server: <code>" + e.getMessage() + "</code>\n" +
                        "Will retry automatically on next schedule interval."
                );
            } else {
                System.out.println("Scheduler Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean isNetworkOrDnsError(Throwable t) {
        while (t != null) {
            if (t instanceof java.net.UnknownHostException || t instanceof java.net.SocketException || t instanceof java.net.SocketTimeoutException) {
                return true;
            }
            if (t.getMessage() != null && t.getMessage().contains("oauth2.googleapis.com")) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }
}
