package com.example.telegram_bot.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.telegram_bot.model.Deal;
import com.example.telegram_bot.service.AmazonSiteStripeService;
import com.example.telegram_bot.service.TelegramService;

@Component
public class DailyDealFetcherScheduler {

    private final AmazonSiteStripeService amazonSiteStripeService;
    private final TelegramService telegramService;

    @Value("${daily.deal.fetch.enabled:true}")
    private boolean enabled;

    @Value("${daily.deal.fetch.limit:10}")
    private int dailyLimit;

    public DailyDealFetcherScheduler(AmazonSiteStripeService amazonSiteStripeService, TelegramService telegramService) {
        this.amazonSiteStripeService = amazonSiteStripeService;
        this.telegramService = telegramService;
    }

    /**
     * Scheduled job to automatically fetch and save top deals (default 10) daily to Google Sheet.
     * Default schedule: Every day at 08:00 AM (0 0 8 * * ?).
     */
    @Scheduled(cron = "${daily.deal.fetch.cron:0 0 8 * * ?}")
    public void fetchDailyDeals() {
        if (!enabled) {
            System.out.println("ℹ️ Daily deal fetcher is disabled in configuration.");
            return;
        }

        System.out.println("========================================");
        System.out.println("🌅 Daily Deal Fetcher Started");
        System.out.println("Time  : " + LocalDateTime.now());
        System.out.println("Limit : Adding " + dailyLimit + " deals to Google Sheet");
        System.out.println("========================================");

        try {
            List<Deal> deals = amazonSiteStripeService.scrapeGoldboxTopDeals("https://www.amazon.in/gp/goldbox", dailyLimit);
            System.out.println("✅ Successfully added " + deals.size() + " top daily deals to Google Sheet!");
            
            telegramService.sendAdminNotification(
                    "🌅 <b>Daily Deal Fetcher Summary</b>\n\n" +
                    "✅ Successfully scraped and added <b>" + deals.size() + "</b> new products to Google Sheet.\n" +
                    "⏰ Next scheduled run tomorrow at 8:00 AM."
            );
        } catch (Exception e) {
            System.err.println("❌ Daily Deal Fetcher Error: " + e.getMessage());
            telegramService.sendAdminNotification(
                    "❌ <b>Daily Deal Fetcher Error Alert</b>\n\n" +
                    "⚠️ Failed to fetch daily deals: <code>" + e.getMessage() + "</code>"
            );
        }

        System.out.println("========================================");
        System.out.println("Daily Deal Fetcher Finished");
        System.out.println("========================================");
    }
}
