package com.example.telegram_bot.scheduler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.telegram_bot.model.Deal;
import com.example.telegram_bot.model.ProductCategory;
import com.example.telegram_bot.service.CarouselService;
import com.example.telegram_bot.service.FacebookPageService;
import com.example.telegram_bot.service.GoogleSheetService;
import com.example.telegram_bot.service.InstagramService;
import com.example.telegram_bot.service.TelegramService;
import com.example.telegram_bot.service.VideoGenerationService;

@Component
public class SocialMediaScheduleScheduler {

    private final GoogleSheetService googleSheetService;
    private final InstagramService instagramService;
    private final FacebookPageService facebookPageService;
    private final CarouselService carouselService;
    private final VideoGenerationService videoGenerationService;
    private final TelegramService telegramService;
    private final com.example.telegram_bot.service.DealScoreService dealScoreService;
    private final com.example.telegram_bot.service.YouTubeService youtubeService;

    @Value("${app.server.base-url:http://localhost:8080}")
    private String serverBaseUrl;

    @Value("${social.schedule.enabled:true}")
    private boolean scheduleEnabled;

    // Daily metrics counters
    private final AtomicInteger dailyStoriesCount = new AtomicInteger(0);
    private final AtomicInteger dailyReelsCount = new AtomicInteger(0);
    private final AtomicInteger dailyCarouselCount = new AtomicInteger(0);
    private final AtomicInteger dailyShortsCount = new AtomicInteger(0);
    private final AtomicInteger reelFormatPointer = new AtomicInteger(0);
    private LocalDateTime lastRunTime;

    private static final String[] CAROUSEL_CATEGORIES = {"bluetooth", "watch", "laptop", "mobile", "shoe"};
    private int categoryPointer = 0;

    public SocialMediaScheduleScheduler(
            GoogleSheetService googleSheetService,
            InstagramService instagramService,
            FacebookPageService facebookPageService,
            CarouselService carouselService,
            VideoGenerationService videoGenerationService,
            TelegramService telegramService,
            com.example.telegram_bot.service.DealScoreService dealScoreService,
            com.example.telegram_bot.service.YouTubeService youtubeService) {
        this.googleSheetService = googleSheetService;
        this.instagramService = instagramService;
        this.facebookPageService = facebookPageService;
        this.carouselService = carouselService;
        this.videoGenerationService = videoGenerationService;
        this.telegramService = telegramService;
        this.dealScoreService = dealScoreService;
        this.youtubeService = youtubeService;
    }

    // ==========================================
    // 1. OFFER REELS (2 per day with format rotation)
    // Slot 1: 11:30 AM IST
    // Slot 2: 07:30 PM IST
    // ==========================================

    @Scheduled(cron = "${social.schedule.reels.cron1:0 30 11 * * ?}", zone = "${social.schedule.zone:Asia/Kolkata}")
    public void executeReelSlot1() {
        if (!scheduleEnabled) return;
        System.out.println("🎬 [Schedule] Triggering Daily Reel Slot 1 (11:30 AM IST)...");
        triggerReelPosting("Slot 1 (11:30 AM)");
    }

    @Scheduled(cron = "${social.schedule.reels.cron2:0 30 19 * * ?}", zone = "${social.schedule.zone:Asia/Kolkata}")
    public void executeReelSlot2() {
        if (!scheduleEnabled) return;
        System.out.println("🎬 [Schedule] Triggering Daily Reel Slot 2 (07:30 PM IST)...");
        triggerReelPosting("Slot 2 (07:30 PM)");
    }

    // ==========================================
    // 2. OFFER / CAROUSEL POSTS (1 per day)
    // Slot 1: 03:00 PM IST
    // ==========================================

    @Scheduled(cron = "${social.schedule.carousel.cron:0 0 15 * * ?}", zone = "${social.schedule.zone:Asia/Kolkata}")
    public void executeCarouselSlot() {
        if (!scheduleEnabled) return;
        System.out.println("🖼️ [Schedule] Triggering Daily Carousel Post (03:00 PM IST)...");
        triggerCarouselPosting("03:00 PM");
    }

    // ==========================================
    // 3. STORIES (6 per day - matching 5-10 target)
    // Slot 1: 09:00 AM IST
    // Slot 2: 11:30 AM IST
    // Slot 3: 02:00 PM IST
    // Slot 4: 04:30 PM IST
    // Slot 5: 07:30 PM IST
    // Slot 6: 09:30 PM IST
    // ==========================================

    @Scheduled(cron = "${social.schedule.stories.cron1:0 0 9 * * ?}", zone = "${social.schedule.zone:Asia/Kolkata}")
    public void executeStorySlot1() {
        if (!scheduleEnabled) return;
        System.out.println("📱 [Schedule] Triggering Daily Story Slot 1 (09:00 AM IST)...");
        triggerStoryPosting("Slot 1 (09:00 AM)");
    }

    @Scheduled(cron = "${social.schedule.stories.cron2:0 30 11 * * ?}", zone = "${social.schedule.zone:Asia/Kolkata}")
    public void executeStorySlot2() {
        if (!scheduleEnabled) return;
        System.out.println("📱 [Schedule] Triggering Daily Story Slot 2 (11:30 AM IST)...");
        triggerStoryPosting("Slot 2 (11:30 AM)");
    }

    @Scheduled(cron = "${social.schedule.stories.cron3:0 0 14 * * ?}", zone = "${social.schedule.zone:Asia/Kolkata}")
    public void executeStorySlot3() {
        if (!scheduleEnabled) return;
        System.out.println("📱 [Schedule] Triggering Daily Story Slot 3 (02:00 PM IST)...");
        triggerStoryPosting("Slot 3 (02:00 PM)");
    }

    @Scheduled(cron = "${social.schedule.stories.cron4:0 30 16 * * ?}", zone = "${social.schedule.zone:Asia/Kolkata}")
    public void executeStorySlot4() {
        if (!scheduleEnabled) return;
        System.out.println("📱 [Schedule] Triggering Daily Story Slot 4 (04:30 PM IST)...");
        triggerStoryPosting("Slot 4 (04:30 PM)");
    }

    @Scheduled(cron = "${social.schedule.stories.cron5:0 30 19 * * ?}", zone = "${social.schedule.zone:Asia/Kolkata}")
    public void executeStorySlot5() {
        if (!scheduleEnabled) return;
        System.out.println("📱 [Schedule] Triggering Daily Story Slot 5 (07:30 PM IST)...");
        triggerStoryPosting("Slot 5 (07:30 PM)");
    }

    @Scheduled(cron = "${social.schedule.stories.cron6:0 30 21 * * ?}", zone = "${social.schedule.zone:Asia/Kolkata}")
    public void executeStorySlot6() {
        if (!scheduleEnabled) return;
        System.out.println("📱 [Schedule] Triggering Daily Story Slot 6 (09:30 PM IST)...");
        triggerStoryPosting("Slot 6 (09:30 PM)");
    }

    // ==========================================
    // 4. YOUTUBE SHORTS (1 per day - 01:30 PM IST Lunch Window)
    // ==========================================
    @Scheduled(cron = "${social.schedule.youtube.cron:0 30 13 * * ?}", zone = "${social.schedule.zone:Asia/Kolkata}")
    public void executeYouTubeShortSlot() {
        if (!scheduleEnabled) return;
        System.out.println("🔴 [Schedule] Triggering Daily YouTube Short (01:30 PM IST)...");
        triggerYouTubeShortPosting("01:30 PM");
    }

    // Reset daily counters at Midnight IST
    @Scheduled(cron = "0 0 0 * * ?", zone = "${social.schedule.zone:Asia/Kolkata}")
    public void resetDailyCounters() {
        dailyStoriesCount.set(0);
        dailyReelsCount.set(0);
        dailyCarouselCount.set(0);
        dailyShortsCount.set(0);
        System.out.println("🔄 Daily social schedule metrics reset for new day.");
    }

    // ==========================================
    // CORE POSTING HELPERS
    // ==========================================

    public boolean triggerReelPosting(String slotLabel) {
        this.lastRunTime = LocalDateTime.now();
        try {
            Deal deal = getNextDealForPosting();
            if (deal == null) {
                System.out.println("⚠️ No valid deal found to post as Reel for " + slotLabel);
                return false;
            }

            // Generate Reel video with dynamic format hook rotation
            int formatIdx = reelFormatPointer.getAndIncrement() % 5;
            String reelPath = videoGenerationService.createReel(deal, formatIdx);
            String videoUrl = serverBaseUrl + "/video/stream";
            String hookText = videoGenerationService.generateReelHookText(deal, formatIdx);

            System.out.println("🎬 Posting Reel for deal: " + deal.getTitle() + " [Hook: " + hookText + "]");
            boolean igSuccess = instagramService.publishReel(deal, videoUrl);
            boolean fbSuccess = facebookPageService.publishReel(deal, videoUrl);

            if (igSuccess || fbSuccess) {
                dailyReelsCount.incrementAndGet();
                telegramService.sendAdminNotification(
                        "🎬 <b>Daily Offer Reel Posted (" + slotLabel + ")</b>\n\n" +
                        "🔥 <b>Hook:</b> " + hookText + "\n" +
                        "<b>Title:</b> " + deal.getTitle() + "\n" +
                        "💰 <b>Price:</b> ₹" + deal.getPrice() + "\n" +
                        "📲 <b>Instagram Reel:</b> " + (igSuccess ? "✅ Success" : "❌ Failed") + "\n" +
                        "📘 <b>Facebook Reel:</b> " + (fbSuccess ? "✅ Success" : "❌ Failed")
                );
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error posting Reel: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean triggerCarouselPosting(String slotLabel) {
        this.lastRunTime = LocalDateTime.now();

        try {
            String selectedCategory = CAROUSEL_CATEGORIES[categoryPointer % CAROUSEL_CATEGORIES.length];
            categoryPointer++;

            List<Deal> deals = carouselService.getDealsForCategoryName(selectedCategory);
            if (deals == null || deals.isEmpty()) {
                System.out.println("⚠️ No deals found for category '" + selectedCategory + "'. Trying next category...");
                for (String cat : CAROUSEL_CATEGORIES) {
                    deals = carouselService.getDealsForCategoryName(cat);
                    if (deals != null && !deals.isEmpty()) {
                        selectedCategory = cat;
                        break;
                    }
                }
            }

            if (deals == null || deals.isEmpty()) {
                System.out.println("⚠️ No active deals available for Carousel posting.");
                return false;
            }

            int count = Math.min(5, deals.size());
            List<Deal> carouselDeals = deals.subList(0, count);
            ProductCategory catEnum = carouselDeals.get(0) != null ? carouselService.getDealsForCategoryName(selectedCategory).size() > 0 ? ProductCategory.DEFAULT : ProductCategory.DEFAULT : ProductCategory.DEFAULT;
            String caption = carouselService.buildCarouselCaption(catEnum, carouselDeals);

            boolean igSuccess = instagramService.publishInstagramCarousel(carouselDeals, caption);
            boolean fbSuccess = facebookPageService.publishFacebookCarousel(carouselDeals, caption);

            if (igSuccess || fbSuccess) {
                dailyCarouselCount.incrementAndGet();
                telegramService.sendAdminNotification(
                        "🖼️ <b>Daily Offer Carousel Posted (" + slotLabel + ")</b>\n\n" +
                        "🏷️ <b>Category:</b> " + selectedCategory.toUpperCase() + " (" + count + " items)\n" +
                        "📲 <b>Instagram Carousel:</b> " + (igSuccess ? "✅ Success" : "❌ Failed") + "\n" +
                        "📘 <b>Facebook Carousel:</b> " + (fbSuccess ? "✅ Success" : "❌ Failed")
                );
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error posting Carousel: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean triggerStoryPosting(String slotLabel) {
        this.lastRunTime = LocalDateTime.now();

        try {
            Deal deal = getNextDealForPosting();
            if (deal == null) {
                System.out.println("⚠️ No deal available to publish Story for " + slotLabel);
                return false;
            }

            System.out.println("📱 Publishing Story for deal: " + deal.getTitle());
            try {
                videoGenerationService.createPostImage(deal);
            } catch (Exception e) {
                System.out.println("⚠️ Could not pre-generate story image with price overlay: " + e.getMessage());
            }
            boolean igSuccess = instagramService.publishStory(deal);
            boolean fbSuccess = facebookPageService.publishStory(deal);

            if (igSuccess || fbSuccess) {
                dailyStoriesCount.incrementAndGet();
                telegramService.sendAdminNotification(
                        "📱 <b>Daily Offer Story Posted (" + slotLabel + ")</b>\n\n" +
                        "<b>Title:</b> " + deal.getTitle() + "\n" +
                        "💰 <b>Price:</b> ₹" + deal.getPrice() + "\n" +
                        "📲 <b>Instagram Story:</b> " + (igSuccess ? "✅ Success" : "❌ Failed") + "\n" +
                        "📘 <b>Facebook Story:</b> " + (fbSuccess ? "✅ Success" : "❌ Failed")
                );
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error publishing Story: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public Map<String, Object> triggerYouTubeShortPosting(String slotLabel) {
        this.lastRunTime = LocalDateTime.now();
        try {
            Map<String, Object> uploadResult = youtubeService.uploadTopDealShortAutomatically();
            String status = (String) uploadResult.getOrDefault("status", "UNKNOWN");
            if ("SUCCESS".equalsIgnoreCase(status)) {
                dailyShortsCount.incrementAndGet();
                String shortsUrl = (String) uploadResult.get("shortsUrl");
                String dealTitle = (String) uploadResult.get("dealTitle");
                System.out.println("✅ [Schedule] YouTube Short posted: " + shortsUrl);
                telegramService.sendAdminNotification(
                        "🎬 <b>YouTube Short Published (" + slotLabel + ")</b>\n\n" +
                        "📦 <b>Deal:</b> " + dealTitle + "\n" +
                        "🔗 <b>Watch:</b> <a href=\"" + shortsUrl + "\">" + shortsUrl + "</a>\n" +
                        "📌 <b>Affiliate Comment:</b> " + (Boolean.TRUE.equals(uploadResult.get("affiliateCommentPosted")) ? "✅ Added" : "⚠️ Skipped"));
            } else if ("DISABLED".equalsIgnoreCase(status)) {
                System.out.println("ℹ️ [Schedule] YouTube auto-upload skipped: " + uploadResult.get("message"));
            } else {
                System.err.println("⚠️ [Schedule] YouTube Short upload failed: " + uploadResult);
            }
            return uploadResult;
        } catch (Exception e) {
            System.err.println("❌ [Schedule] YouTube Short scheduling error: " + e.getMessage());
            telegramService.sendAdminNotification("⚠️ <b>YouTube Short Auto-Upload Error:</b> " + e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return error;
        }
    }

    private Deal getNextDealForPosting() {
        try {
            List<List<Object>> rows = googleSheetService.getAllRows();
            if (rows == null || rows.isEmpty()) return null;

            List<Deal> candidates = new java.util.ArrayList<>();
            for (List<Object> row : rows) {
                String title = row.size() > 0 ? row.get(0).toString().trim() : "";
                String price = row.size() > 1 ? row.get(1).toString().trim() : "";
                String image = row.size() > 2 ? row.get(2).toString().trim() : "";
                String link = row.size() > 3 ? row.get(3).toString().trim() : "";
                String source = row.size() > 4 ? row.get(4).toString().trim() : "Amazon";

                if (title.isEmpty() || title.equalsIgnoreCase("Amazon Deal") || price.isEmpty() || price.equalsIgnoreCase("N/A") || image.isEmpty() || link.isEmpty()) {
                    continue;
                }

                Deal deal = new Deal();
                deal.setTitle(title);
                deal.setPrice(price);
                deal.setImage(image);
                deal.setLink(link);
                deal.setSource(source);
                candidates.add(deal);
            }

            if (!candidates.isEmpty()) {
                Deal topDeal = dealScoreService.getTopRankedDeal(candidates);
                if (topDeal != null) {
                    System.out.printf("🏆 Selected Top Deal by Score: [%s] (Deal Score: %.1f | Disc: %.0f | Price: %.0f | Pop: %.0f | Cat: %.0f)%n",
                            topDeal.getTitle(), topDeal.getDealScore(), topDeal.getDiscountScore(),
                            topDeal.getPriceAttractivenessScore(), topDeal.getProductPopularityScore(), topDeal.getCategoryDemandScore());
                }
                return topDeal;
            }
        } catch (Exception e) {
            System.err.println("Error fetching next deal for schedule: " + e.getMessage());
        }
        return null;
    }

    public Map<String, Object> getScheduleMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("scheduleEnabled", scheduleEnabled);
        metrics.put("dailyTarget", Map.of(
                "reelsPerDay", "2 (Rotates 5 Dynamic Hook Formats)",
                "youtubeShortsPerDay", "1 (Auto-upload + Pinned Affiliate Link)",
                "carouselPostsPerDay", "0-1 (Category Focus)",
                "storiesPerDay", "5-10 (Configured: 6/day)",
                "staticPostsPerDay", "0-1 (Loot Deals >=60% OFF)"
        ));
        metrics.put("todayCompleted", Map.of(
                "reels", dailyReelsCount.get(),
                "youtubeShorts", dailyShortsCount.get(),
                "carousels", dailyCarouselCount.get(),
                "stories", dailyStoriesCount.get()
        ));
        metrics.put("lastExecutionTime", lastRunTime != null ? lastRunTime.toString() : "N/A");
        metrics.put("scheduleSlots", Map.of(
                "reels", List.of("11:30 AM IST", "07:30 PM IST"),
                "youtubeShorts", List.of("01:30 PM IST"),
                "carousel", List.of("03:00 PM IST"),
                "stories", List.of("09:00 AM IST", "11:30 AM IST", "02:00 PM IST", "04:30 PM IST", "07:30 PM IST", "09:30 PM IST")
        ));
        return metrics;
    }
}
