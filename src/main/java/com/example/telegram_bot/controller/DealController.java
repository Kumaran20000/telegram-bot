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

import java.util.HashMap;
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

    @Data
    public static class SiteStripeRequest {
        private String url;
        private String title;
        private String price;
        private String image;
        private String link;
        private String html;
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
}
