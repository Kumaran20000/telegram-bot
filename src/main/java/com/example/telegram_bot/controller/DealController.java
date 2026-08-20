package com.example.telegram_bot.controller;

import com.example.telegram_bot.model.Deal;
import com.example.telegram_bot.model.ProductCategory;
import com.example.telegram_bot.service.AmazonSiteStripeService;
import com.example.telegram_bot.service.CaptionService;
import com.example.telegram_bot.service.CarouselService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/deals")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allows Chrome extension requests
public class DealController {

    private final AmazonSiteStripeService siteStripeService;
    private final CaptionService captionService;
    private final CarouselService carouselService;
    private final com.example.telegram_bot.service.FacebookPageService facebookPageService;
    private final com.example.telegram_bot.service.InstagramService instagramService;
    private final com.example.telegram_bot.service.GoogleSheetService googleSheetService;
    private final com.example.telegram_bot.scheduler.SocialMediaScheduleScheduler socialMediaScheduleScheduler;
    private final com.example.telegram_bot.service.DealScoreService dealScoreService;
    private final com.example.telegram_bot.service.HashtagService hashtagService;
    private final com.example.telegram_bot.service.CategoryService categoryService;
    private final com.example.telegram_bot.service.InstagramSheetService instagramSheetService;
    private final com.example.telegram_bot.service.TrendingInstagramAudioService trendingAudioService;
    private final com.example.telegram_bot.service.MessageFormatterService messageFormatterService;
    private final com.example.telegram_bot.service.YouTubeService youtubeService;

    @Data
    public static class SiteStripeRequest {
        private String url;
        private String title;
        private String price;
        private String image;
        private String link;
        private String html;
    }

    @Data
    public static class InstagramDealRequest {
        private String title;
        private String price;
        private String image;
        private String link;
        private String group;
        private String targetType; // CAROUSEL, REEL, BOTH
        private String source;
    }

    /**
     * Endpoint to add a product to Google Sheet by Amazon URL or SiteStripe link.
     * Automatically extracts missing details (Title, Price, Image) from Amazon.
     */
    @PostMapping("/add-by-url")
    public ResponseEntity<?> addDealByUrl(@RequestBody SiteStripeRequest request) {
        try {
            String targetUrl = request.getUrl() != null ? request.getUrl() : request.getLink();
            if (targetUrl == null && request.getHtml() != null) {
                targetUrl = request.getHtml();
            }

            Deal deal = siteStripeService.processAndSaveSiteStripe(
                    targetUrl,
                    request.getTitle(),
                    request.getPrice(),
                    request.getImage()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Product successfully scraped and added to Google Sheet");
            response.put("deal", deal);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Endpoint tailored for Chrome SiteStripe extension / payload format.
     */
    @PostMapping("/sitestripe")
    public ResponseEntity<?> addSiteStripeDeal(@RequestBody SiteStripeRequest request) {
        return addDealByUrl(request);
    }

    /**
     * Preview extracted product details from Amazon URL without writing to Google Sheet.
     */
    @GetMapping("/preview-url")
    public ResponseEntity<?> previewUrl(@RequestParam String url) {
        try {
            String expanded = siteStripeService.expandUrl(siteStripeService.extractUrl(url));
            Deal deal = siteStripeService.scrapeAmazonProduct(expanded);
            deal.setLink(url);
            return ResponseEntity.ok(deal);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Scan existing rows in Google Sheet with missing product details and populate them from Amazon.
     */
    @PostMapping("/enrich-sheet")
    public ResponseEntity<?> enrichSheet() {
        try {
            int count = siteStripeService.enrichSheetDeals();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("updatedRows", count);
            response.put("message", "Enriched " + count + " Google Sheet rows with missing product details.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Trigger daily fetch of N top deals (default 10) into Google Sheet on-demand.
     */
    @PostMapping("/daily-fetch")
    public ResponseEntity<?> fetchDailyDeals(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<Deal> deals = siteStripeService.scrapeGoldboxTopDeals("https://www.amazon.in/gp/goldbox", limit);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("count", deals.size());
            response.put("limit", limit);
            response.put("message", "Successfully fetched and saved " + deals.size() + " daily products to Google Sheet.");
            response.put("deals", deals);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Scrape top N deals from Amazon Goldbox / Today's Deals (https://www.amazon.in/gp/goldbox) and save them to Google Sheet.
     */
    @PostMapping("/goldbox")
    public ResponseEntity<?> scrapeGoldboxDeals(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String url) {
        try {
            String goldboxUrl = (url != null && !url.trim().isEmpty()) ? url : "https://www.amazon.in/gp/goldbox";
            List<Deal> deals = siteStripeService.scrapeGoldboxTopDeals(goldboxUrl, limit);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("count", deals.size());
            response.put("message", "Successfully scraped and saved " + deals.size() + " Goldbox deals to Google Sheet.");
            response.put("deals", deals);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Generate product-tailored Instagram caption with category detection and hashtags.
     */
    @GetMapping("/caption")
    public ResponseEntity<?> generateCaption(
            @RequestParam String title,
            @RequestParam(defaultValue = "N/A") String price,
            @RequestParam(required = false) Integer templateIndex) {
        Deal deal = new Deal();
        deal.setTitle(title);
        deal.setPrice(price);

        String caption = (templateIndex != null)
                ? captionService.createCaption(deal, templateIndex)
                : captionService.createCaption(deal);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("title", title);
        response.put("price", price);
        if (templateIndex != null) {
            response.put("templateIndex", templateIndex % 10);
        }
        response.put("caption", caption);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all Google Sheet deals grouped by Product Category (e.g. HEADPHONE / Bluetooth, WATCH, LAPTOP, etc.).
     */
    @GetMapping("/grouped")
    public ResponseEntity<?> getDealsGrouped() {
        try {
            Map<ProductCategory, List<Deal>> groupedMap = carouselService.groupDealsByCategory();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("groupedDeals", groupedMap);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get carousel data for a specific product category (e.g. "bluetooth", "watch", "laptop").
     */
    @GetMapping("/carousel-by-category")
    public ResponseEntity<?> getCarouselByCategory(@RequestParam String category) {
        try {
            List<Deal> deals = carouselService.getDealsForCategoryName(category);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("category", category);
            response.put("count", deals.size());
            response.put("deals", deals);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Post a grouped multi-slide product carousel (e.g., Bluetooth earbuds, Watches) exclusively to Instagram.
     */
    @PostMapping("/post-carousel")
    public ResponseEntity<?> postCarousel(@RequestParam String category) {
        try {
            boolean posted = carouselService.postCategoryCarouselToInstagram(category);
            Map<String, Object> response = new HashMap<>();
            response.put("status", posted ? "SUCCESS" : "FAILED");
            response.put("platform", "Instagram");
            response.put("category", category);
            response.put("message", posted ? "Carousel successfully posted to Instagram" : "Failed to post Instagram carousel (check Graph API credentials / image URLs)");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Accumulates 3 to 5 deals for a category (e.g. laptop, shoe, watch, phone)
     * and returns a formatted DM message with direct purchase links ready to be sent to user DMs.
     */
    @GetMapping("/category-dm")
    public ResponseEntity<?> getCategoryDmContent(
            @RequestParam String category,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            Map<String, Object> response = carouselService.getCategoryDmContent(category, limit);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Posts an accumulated category deals group (3 to 5 deals) to Telegram or Instagram on-demand.
     */
    @PostMapping("/post-category-group")
    public ResponseEntity<?> postCategoryGroup(
            @RequestParam String category,
            @RequestParam(defaultValue = "both") String platform,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            boolean telegramResult = false;
            boolean instagramResult = false;

            if ("telegram".equalsIgnoreCase(platform) || "both".equalsIgnoreCase(platform)) {
                telegramResult = carouselService.postCategoryGroupToTelegram(category, limit);
            }
            if ("instagram".equalsIgnoreCase(platform) || "both".equalsIgnoreCase(platform)) {
                instagramResult = carouselService.postCategoryCarouselToInstagram(category, limit);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("category", category);
            response.put("limit", limit);
            response.put("telegramPosted", telegramResult);
            response.put("instagramPosted", instagramResult);
            response.put("message", "Accumulated top " + limit + " deals for category '" + category + "' and processed posting.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Test endpoint to publish a deal to a Facebook Page on demand.
     */
    @PostMapping("/post-facebook")
    public ResponseEntity<?> postToFacebook(@RequestBody(required = false) SiteStripeRequest request) {
        try {
            Deal deal;
            if (request != null && (request.getUrl() != null || request.getLink() != null)) {
                String targetUrl = request.getUrl() != null ? request.getUrl() : request.getLink();
                deal = siteStripeService.processAndSaveSiteStripe(targetUrl, request.getTitle(), request.getPrice(), request.getImage());
            } else {
                deal = new Deal();
                deal.setTitle((request != null && request.getTitle() != null) ? request.getTitle() : "Sample Amazon Deal");
                deal.setPrice((request != null && request.getPrice() != null) ? request.getPrice() : "999");
                deal.setImage((request != null && request.getImage() != null) ? request.getImage() : "https://dummyimage.com/600x600/ffffff/000000.jpg&text=Amazon+Deal");
                deal.setLink((request != null && request.getLink() != null) ? request.getLink() : "https://www.amazon.in");
                deal.setSource("Amazon");
            }

            boolean posted = facebookPageService.publish(deal);
            Map<String, Object> response = new HashMap<>();
            response.put("status", posted ? "SUCCESS" : "FAILED");
            response.put("platform", "Facebook Page");
            response.put("deal", deal);
            response.put("message", posted ? "Successfully posted to Facebook Page" : "Failed to post to Facebook Page (check page ID / access token)");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Diagnostic endpoint to check Facebook Page Access Token status.
     */
    @GetMapping("/facebook-token-status")
    public ResponseEntity<?> checkFacebookTokenStatus() {
        return ResponseEntity.ok(facebookPageService.checkTokenStatus());
    }

    /**
     * Diagnostic endpoint to check daily social media schedule status & progress metrics.
     */
    @GetMapping("/schedule-status")
    public ResponseEntity<?> getScheduleStatus() {
        return ResponseEntity.ok(socialMediaScheduleScheduler.getScheduleMetrics());
    }

    /**
     * Trigger an Offer Reel to Facebook & Instagram on demand.
     */
    @PostMapping("/post-reel")
    public ResponseEntity<?> triggerReelPost(@RequestParam(defaultValue = "Manual API Trigger") String slotLabel) {
        boolean success = socialMediaScheduleScheduler.triggerReelPosting(slotLabel);
        Map<String, Object> response = new HashMap<>();
        response.put("status", success ? "SUCCESS" : "FAILED");
        response.put("message", success ? "Offer Reel successfully generated and posted to Facebook & Instagram!" : "Failed to post Offer Reel (check logs/credentials)");
        return ResponseEntity.ok(response);
    }

    /**
     * Trigger an Offer Story to Facebook & Instagram on demand.
     */
    @PostMapping("/post-story")
    public ResponseEntity<?> triggerStoryPost(@RequestParam(defaultValue = "Manual API Trigger") String slotLabel) {
        boolean success = socialMediaScheduleScheduler.triggerStoryPosting(slotLabel);
        Map<String, Object> response = new HashMap<>();
        response.put("status", success ? "SUCCESS" : "FAILED");
        response.put("message", success ? "Offer Story successfully posted to Facebook & Instagram!" : "Failed to post Offer Story (check logs/credentials)");
        return ResponseEntity.ok(response);
    }

    /**
     * Get all Google Sheet deals ranked by Deal Score with score breakdown.
     * Formula: Deal Score = Discount % + Price Attractiveness + Product Popularity + Category Demand + Previous Performance Score
     */
    @GetMapping("/ranked")
    public ResponseEntity<?> getDealsRankedByScore() {
        try {
            List<Deal> deals = carouselService.getDealsForCategoryName("");
            List<Deal> rankedDeals = dealScoreService.rankDeals(deals);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("formula", "Deal Score = Discount % + Price Attractiveness + Product Popularity + Category Demand + Previous Performance Score");
            response.put("count", rankedDeals.size());
            response.put("rankedDeals", rankedDeals);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Generate dynamic product-tailored hashtags based on product title, category, price, and discount.
     */
    @GetMapping("/hashtags")
    public ResponseEntity<?> getProductHashtags(
            @RequestParam String title,
            @RequestParam(required = false) String price,
            @RequestParam(required = false) String mrp,
            @RequestParam(required = false) String discount,
            @RequestParam(required = false) String category) {
        try {
            ProductCategory productCategory;
            if (category != null && !category.trim().isEmpty()) {
                try {
                    productCategory = ProductCategory.valueOf(category.toUpperCase().trim());
                } catch (Exception e) {
                    productCategory = categoryService.detectCategory(category);
                }
            } else {
                productCategory = categoryService.detectCategory(title);
            }

            Map<String, Object> response = hashtagService.generateProductHashtagDetails(
                    title, price, mrp, discount, productCategory);
            response.put("status", "SUCCESS");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get all distinct product groups / collections from the dedicated Instagram sheet tab.
     */
    @GetMapping("/instagram-sheet/groups")
    public ResponseEntity<?> getInstagramSheetGroups() {
        try {
            Map<String, List<com.example.telegram_bot.model.InstagramDealItem>> grouped = 
                    instagramSheetService.getDealsGroupedByCollection();

            Map<String, Object> groupSummaries = new LinkedHashMap<>();
            int totalItems = 0;

            for (Map.Entry<String, List<com.example.telegram_bot.model.InstagramDealItem>> entry : grouped.entrySet()) {
                List<com.example.telegram_bot.model.InstagramDealItem> items = entry.getValue();
                totalItems += items.size();

                long newCount = items.stream().filter(i -> "NEW".equalsIgnoreCase(i.getStatus())).count();
                long postedCount = items.stream().filter(i -> "POSTED".equalsIgnoreCase(i.getStatus())).count();

                Map<String, Object> summary = new HashMap<>();
                summary.put("itemCount", items.size());
                summary.put("newCount", newCount);
                summary.put("postedCount", postedCount);
                summary.put("deals", items);

                groupSummaries.put(entry.getKey(), summary);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("totalDealsInInstagramSheet", totalItems);
            response.put("totalGroups", grouped.size());
            response.put("groups", groupSummaries);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get all deals belonging to a specific group / collection in the Instagram sheet tab.
     */
    @GetMapping("/instagram-sheet/deals")
    public ResponseEntity<?> getInstagramSheetDeals(
            @RequestParam String group,
            @RequestParam(defaultValue = "false") boolean onlyNew) {
        try {
            List<com.example.telegram_bot.model.InstagramDealItem> deals = 
                    instagramSheetService.getDealsForGroup(group, onlyNew);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("group", group);
            response.put("onlyNew", onlyNew);
            response.put("count", deals.size());
            response.put("deals", deals);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Add a deal directly to the dedicated Instagram sheet tab with a custom group label.
     */
    @PostMapping("/instagram-sheet/add")
    public ResponseEntity<?> addDealToInstagramSheet(@RequestBody InstagramDealRequest request) {
        try {
            Deal deal = new Deal();
            deal.setTitle(request.getTitle());
            deal.setPrice(request.getPrice());
            deal.setImage(request.getImage());
            deal.setLink(request.getLink());
            deal.setSource(request.getSource() != null ? request.getSource() : "Amazon");

            boolean saved = instagramSheetService.saveInstagramDeal(
                    deal, request.getGroup(), request.getTargetType());

            Map<String, Object> response = new HashMap<>();
            response.put("status", saved ? "SUCCESS" : "DUPLICATE_OR_FAILED");
            response.put("message", saved ? "Deal saved to Instagram sheet under group: " + request.getGroup() : "Deal already exists in Instagram sheet or invalid.");
            response.put("deal", deal);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Synchronize and auto-categorize deals from the main sheet (Sheet1) into the Instagram sheet tab.
     */
    @PostMapping("/instagram-sheet/sync-from-main")
    public ResponseEntity<?> syncMainSheetToInstagram(@RequestParam(defaultValue = "50") int limit) {
        try {
            int synced = instagramSheetService.syncFromMainSheet(limit);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("syncedCount", synced);
            response.put("message", "Successfully synced " + synced + " deals from main sheet into Instagram sheet grouped by category.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Post a grouped multi-slide Carousel (e.g. Headsets, Best Gadgets) to Instagram from the Instagram sheet.
     */
    @PostMapping("/instagram-sheet/post-carousel")
    public ResponseEntity<?> postGroupCarousel(
            @RequestParam String group,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            Map<String, Object> result = instagramSheetService.postGroupCarouselToInstagram(group, limit);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Post a dynamic Video Reel for the top deal of a group (e.g. Best Gadgets) to Instagram from the Instagram sheet.
     */
    @PostMapping("/instagram-sheet/post-reel")
    public ResponseEntity<?> postGroupReel(@RequestParam String group) {
        try {
            Map<String, Object> result = instagramSheetService.postGroupReelToInstagram(group);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Retrieves the curated catalog of upbeat, energetic Instagram Reels audio tracks (showing ↗️ Trending Indicator).
     */
    @GetMapping("/trending-audio")
    public ResponseEntity<?> getTrendingAudioLibrary(@RequestParam(required = false) String category) {
        try {
            ProductCategory cat = null;
            if (category != null && !category.trim().isEmpty()) {
                try {
                    cat = ProductCategory.valueOf(category.toUpperCase().trim());
                } catch (Exception e) {
                    cat = categoryService.detectCategory(category);
                }
            }

            List<com.example.telegram_bot.model.TrendingAudioTrack> tracks = (cat != null)
                    ? trendingAudioService.getTracksForCategory(cat)
                    : trendingAudioService.getAllTracks();

            List<Map<String, Object>> result = new ArrayList<>();
            for (com.example.telegram_bot.model.TrendingAudioTrack t : tracks) {
                result.add(trendingAudioService.getTrackDetails(t));
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "SUCCESS");
            response.put("totalTracks", result.size());
            response.put("filterCategory", cat != null ? cat.name() : "ALL");
            response.put("selectionCriteria", "Upbeat + Energetic (120-135 BPM) + ↗️ Trending in Reels + Suitable for Product Content");
            response.put("tracks", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Recommends a specific upbeat, energetic trending Instagram sound for a given product deal.
     */
    @GetMapping("/trending-audio/recommend")
    public ResponseEntity<?> recommendTrendingAudio(
            @RequestParam String title,
            @RequestParam(required = false) String price,
            @RequestParam(required = false) String category) {
        try {
            Deal deal = new Deal();
            deal.setTitle(title);
            deal.setPrice(price);

            ProductCategory cat;
            if (category != null && !category.trim().isEmpty()) {
                try {
                    cat = ProductCategory.valueOf(category.toUpperCase().trim());
                } catch (Exception e) {
                    cat = categoryService.detectCategory(category);
                }
            } else {
                cat = categoryService.detectCategory(title);
            }

            com.example.telegram_bot.model.TrendingAudioTrack track = trendingAudioService.getRecommendedTrack(deal, cat);
            Map<String, Object> details = trendingAudioService.getTrackDetails(track);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "SUCCESS");
            response.put("productTitle", title);
            response.put("detectedCategory", cat.name());
            response.put("recommendedAudio", details);
            response.put("captionAdvice", trendingAudioService.formatAudioAdvice(track));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Formats a deal message ready for WhatsApp Channels or WhatsApp groups.
     */
    @GetMapping("/whatsapp-format")
    public ResponseEntity<?> getWhatsAppFormat(
            @RequestParam String title,
            @RequestParam String price,
            @RequestParam(required = false) String mrp,
            @RequestParam(required = false) String link,
            @RequestParam(required = false) String discount) {
        try {
            Deal deal = new Deal();
            deal.setTitle(title);
            deal.setPrice(price);
            deal.setMrp(mrp);
            deal.setLink(link != null ? link : "https://amazon.in");
            if (discount != null) deal.setDiscount(discount);

            String whatsappText = messageFormatterService.formatWhatsAppMessage(deal);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "SUCCESS");
            response.put("whatsappText", whatsappText);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "SUCCESS");
            // Fallback direct format
            StringBuilder sb = new StringBuilder();
            sb.append("🔥 *DEAL ALERT!* 🔥\n\n*").append(title).append("*\n\n💰 *Price:* ₹*").append(price).append("*\n");
            if (mrp != null) sb.append("❌ *MRP:* ~₹").append(mrp).append("~\n");
            if (link != null) sb.append("\n🛒 *BUY NOW ON AMAZON:* ").append(link).append("\n\n");
            sb.append("📲 *Join our Telegram for instant alerts:* https://t.me/BOnlinediscount");
            response.put("whatsappText", sb.toString());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Formats YouTube Shorts SEO Title, Description, Tags, and Pinned Comment for video repurposing.
     */
    @GetMapping("/youtube-shorts-format")
    public ResponseEntity<?> getYouTubeShortsFormat(
            @RequestParam String title,
            @RequestParam String price,
            @RequestParam(required = false) String mrp,
            @RequestParam(required = false) String link,
            @RequestParam(required = false) String discount) {
        Deal deal = new Deal();
        deal.setTitle(title);
        deal.setPrice(price);
        deal.setMrp(mrp);
        deal.setLink(link != null ? link : "https://amazon.in");
        if (discount != null) deal.setDiscount(discount);

        Map<String, Object> shortsPackage = youtubeService.generateShortsPackage(deal);
        return ResponseEntity.ok(shortsPackage);
    }

    /**
     * Automatically picks the top scored deal from Google Sheets, renders its Reel video,
     * and returns the complete ready-to-upload YouTube Shorts package.
     */
    @GetMapping("/youtube-shorts-package/top")
    public ResponseEntity<?> getTopYouTubeShortsPackage() {
        try {
            Map<String, Object> shortsPackage = youtubeService.generateTopDealShortsPackage();
            return ResponseEntity.ok(shortsPackage);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    /**
     * Retrieves the multi-platform growth blueprint, posting schedule, and bio templates.
     */
    @GetMapping("/growth-blueprint")
    public ResponseEntity<?> getGrowthBlueprint() {
        Map<String, Object> blueprint = new LinkedHashMap<>();
        blueprint.put("status", "SUCCESS");

        blueprint.put("instagramBioTemplate", Map.of(
                "line1", "🔥 Daily Amazon Secret Price Drops & 70%+ OFF Deals",
                "line2", "⚡ Tested & Verified Steal Deals",
                "line3", "👇 Tap below for instant loot alerts & purchase links",
                "linkInBio", "https://t.me/BOnlinediscount"
        ));

        blueprint.put("optimalPostingSlotsIST", Map.of(
                "morning (08:30 - 09:30 AM)", "Telegram & WhatsApp (Daily Essentials & Under ₹499)",
                "afternoon (01:00 - 02:00 PM)", "Instagram Reel + Telegram Deal (Gadgets & Tech)",
                "evening (05:30 - 06:30 PM)", "Instagram Carousel (5-10 Slides - High Saves)",
                "peakNight (08:00 - 09:30 PM)", "Flash Sale Reel + Daily Deal Fetcher (Peak Checkout Hours)"
        ));

        blueprint.put("growthFunnelRules", List.of(
                "1. Reels: Hook in 0-2s + ↗️ Upbeat Trending Audio (120-135 BPM) + CTA 'Comment LINK' to drive 10x comments.",
                "2. Carousels: 5-slide category collections + clean price comparisons (no raw HTML tags) + prompt to 'Save this post'.",
                "3. Telegram & WhatsApp: Instant price drop alerts with strike-through MRP and direct buy buttons for repeat conversions.",
                "4. YouTube Shorts: Repurpose the 9:16 vertical MP4 video with pinned comment containing affiliate link + Telegram invite."
        ));

        return ResponseEntity.ok(blueprint);
    }

    /**
     * Automatically uploads top-scoring deal from Google Sheet as a YouTube Short
     * and pins the direct Amazon affiliate comment.
     */
    @PostMapping("/youtube-shorts/post-top")
    public ResponseEntity<?> postTopYouTubeShort() {
        try {
            Map<String, Object> result = youtubeService.uploadTopDealShortAutomatically();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    /**
     * Automatically uploads a specific deal as a YouTube Short and pins the affiliate comment.
     */
    @PostMapping("/youtube-shorts/post")
    public ResponseEntity<?> postYouTubeShort(
            @RequestParam String title,
            @RequestParam String price,
            @RequestParam(required = false) String mrp,
            @RequestParam String link,
            @RequestParam(required = false) String image) {
        try {
            Deal deal = new Deal();
            deal.setTitle(title);
            deal.setPrice(price);
            deal.setMrp(mrp);
            deal.setLink(link);
            deal.setImage(image != null ? image : "");
            deal.setSource("Amazon");

            Map<String, Object> result = youtubeService.uploadShortAutomatically(deal);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    /**
     * Checks YouTube Data API connection, channel name, subscribers, and OAuth token status.
     */
    @GetMapping("/youtube-status")
    public ResponseEntity<?> getYouTubeStatus() {
        Map<String, Object> status = youtubeService.checkYouTubeStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * Manually triggers an instant YouTube Short upload via the scheduler.
     */
    @PostMapping("/schedule/trigger-shorts")
    public ResponseEntity<?> triggerScheduleShorts() {
        Map<String, Object> result = socialMediaScheduleScheduler.triggerYouTubeShortPosting("Manual Trigger");
        return ResponseEntity.ok(result);
    }

    /**
     * Manually triggers an instant Offer Reel post right now.
     */
    @PostMapping("/schedule/trigger-reel")
    public ResponseEntity<?> triggerScheduleReel() {
        boolean success = socialMediaScheduleScheduler.triggerReelPosting("Manual Trigger");
        Map<String, Object> res = new HashMap<>();
        res.put("status", success ? "SUCCESS" : "FAILED");
        res.put("message", success ? "Reel published successfully!" : "Failed to publish Reel (check logs).");
        return ResponseEntity.ok(res);
    }

    /**
     * Manually triggers an instant Carousel post right now.
     */
    @PostMapping("/schedule/trigger-carousel")
    public ResponseEntity<?> triggerScheduleCarousel() {
        boolean success = socialMediaScheduleScheduler.triggerCarouselPosting("Manual Trigger");
        Map<String, Object> res = new HashMap<>();
        res.put("status", success ? "SUCCESS" : "FAILED");
        res.put("message", success ? "Carousel published successfully!" : "Failed to publish Carousel (check logs).");
        return ResponseEntity.ok(res);
    }
}
