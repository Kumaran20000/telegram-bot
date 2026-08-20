package com.example.telegram_bot.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.telegram_bot.model.Deal;
import com.example.telegram_bot.model.InstagramDealItem;
import com.example.telegram_bot.model.ProductCategory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstagramSheetService {

    private final Sheets sheetsService;
    private final InstagramService instagramService;
    private final CategoryService categoryService;
    private final HashtagService hashtagService;
    private final VideoGenerationService videoGenerationService;
    private final DealScoreService dealScoreService;
    private final TrendingInstagramAudioService trendingAudioService;

    @Value("${google.sheet.id}")
    private String spreadsheetId;

    @Value("${instagram.sheet.name:Instagram}")
    private String instagramSheetName;

    @Value("${instagram.sheet.range:Instagram!A2:I}")
    private String instagramSheetRange;

    @Value("${google.sheet.range:Sheet1!A2:H}")
    private String mainSheetRange;

    @Value("${app.server.base-url:http://localhost:8080}")
    private String serverBaseUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Ensures the dedicated Instagram sheet tab exists in the Google Spreadsheet.
     * If missing, creates the tab and initializes the header row.
     */
    public synchronized boolean ensureInstagramSheetExists() {
        try {
            Spreadsheet spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute();
            List<Sheet> sheets = spreadsheet.getSheets();
            boolean tabExists = false;

            if (sheets != null) {
                for (Sheet s : sheets) {
                    if (s.getProperties() != null 
                            && instagramSheetName.equalsIgnoreCase(s.getProperties().getTitle())) {
                        tabExists = true;
                        break;
                    }
                }
            }

            if (!tabExists) {
                System.out.println("📄 Creating dedicated Google Sheet tab: [" + instagramSheetName + "]...");
                AddSheetRequest addSheetRequest = new AddSheetRequest()
                        .setProperties(new SheetProperties().setTitle(instagramSheetName));

                BatchUpdateSpreadsheetRequest batchRequest = new BatchUpdateSpreadsheetRequest()
                        .setRequests(Collections.singletonList(new Request().setAddSheet(addSheetRequest)));

                sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchRequest).execute();

                // Initialize header row
                ValueRange headerBody = new ValueRange().setValues(Collections.singletonList(
                        Arrays.asList("Title", "Price", "Image", "Link", "Group", "Target Type", "Status", "Source", "Date Added")
                ));

                sheetsService.spreadsheets().values()
                        .update(spreadsheetId, instagramSheetName + "!A1:I1", headerBody)
                        .setValueInputOption("RAW")
                        .execute();

                System.out.println("✅ Dedicated Google Sheet tab [" + instagramSheetName + "] created with header row!");
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error ensuring Instagram sheet tab exists: " + e.getMessage());
            return false;
        }
    }

    /**
     * Appends a new deal to the dedicated Instagram sheet tab.
     */
    public boolean saveInstagramDeal(Deal deal, String groupName, String targetType) {
        if (deal == null) return false;
        ensureInstagramSheetExists();

        try {
            if (isDuplicateInstagramDeal(deal)) {
                System.out.println("⚠️ Skipping duplicate Instagram deal: " + deal.getTitle());
                return false;
            }

            String effectiveGroup = (groupName != null && !groupName.trim().isEmpty())
                    ? groupName.trim()
                    : determineSmartGroup(deal);

            String effectiveTarget = (targetType != null && !targetType.trim().isEmpty())
                    ? targetType.toUpperCase().trim()
                    : "BOTH"; // Default to CAROUSEL & REEL

            String dateAdded = LocalDateTime.now().format(DATE_FORMATTER);

            ValueRange appendBody = new ValueRange().setValues(Collections.singletonList(
                    Arrays.asList(
                            deal.getTitle() != null ? deal.getTitle() : "",
                            deal.getPrice() != null ? deal.getPrice() : "",
                            deal.getImage() != null ? deal.getImage() : "",
                            deal.getLink() != null ? deal.getLink() : "",
                            effectiveGroup,
                            effectiveTarget,
                            "NEW",
                            deal.getSource() != null ? deal.getSource() : "Amazon",
                            dateAdded
                    )
            ));

            sheetsService.spreadsheets().values()
                    .append(spreadsheetId, instagramSheetRange, appendBody)
                    .setValueInputOption("RAW")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute();

            System.out.println("✅ Saved deal to Instagram sheet [" + effectiveGroup + "]: " + deal.getTitle());
            return true;
        } catch (Exception e) {
            System.err.println("Error saving to Instagram sheet: " + e.getMessage());
            return false;
        }
    }

    /**
     * Reads all rows from the Instagram sheet tab.
     */
    public List<List<Object>> getAllInstagramRows() {
        ensureInstagramSheetExists();
        try {
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(spreadsheetId, instagramSheetRange)
                    .execute();
            return response.getValues() != null ? response.getValues() : Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Error reading Instagram sheet rows: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Parses rows from the Instagram sheet into structured InstagramDealItem objects.
     */
    public List<InstagramDealItem> getAllInstagramItems() {
        List<List<Object>> rows = getAllInstagramRows();
        List<InstagramDealItem> items = new ArrayList<>();
        int rowNumber = 2; // Row 1 is header

        for (List<Object> row : rows) {
            if (row == null || row.isEmpty()) {
                rowNumber++;
                continue;
            }

            String title = row.size() > 0 ? row.get(0).toString().trim() : "";
            String price = row.size() > 1 ? row.get(1).toString().trim() : "";
            String image = row.size() > 2 ? row.get(2).toString().trim() : "";
            String link = row.size() > 3 ? row.get(3).toString().trim() : "";
            String group = row.size() > 4 ? row.get(4).toString().trim() : "";
            String targetType = row.size() > 5 ? row.get(5).toString().trim() : "BOTH";
            String status = row.size() > 6 ? row.get(6).toString().trim() : "NEW";
            String source = row.size() > 7 ? row.get(7).toString().trim() : "Amazon";
            String dateAdded = row.size() > 8 ? row.get(8).toString().trim() : "";

            if (title.isEmpty() || title.equalsIgnoreCase("Title") || price.isEmpty() || image.isEmpty() || link.isEmpty()) {
                rowNumber++;
                continue;
            }

            Deal deal = new Deal(title, price, image, link, source);
            if (group.isEmpty()) {
                group = determineSmartGroup(deal);
            }

            items.add(new InstagramDealItem(rowNumber, deal, group, targetType, status, dateAdded));
            rowNumber++;
        }
        return items;
    }

    /**
     * Groups all Instagram sheet deals by their Group tag (e.g. "Headsets", "Best Gadgets", "Smartwatches").
     */
    public Map<String, List<InstagramDealItem>> getDealsGroupedByCollection() {
        List<InstagramDealItem> items = getAllInstagramItems();
        Map<String, List<InstagramDealItem>> grouped = new LinkedHashMap<>();

        for (InstagramDealItem item : items) {
            String groupKey = item.getGroup() != null && !item.getGroup().isEmpty() 
                    ? item.getGroup() 
                    : "Best Gadgets";
            grouped.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(item);
        }
        return grouped;
    }

    /**
     * Retrieves all deals belonging to a specific group name (case-insensitive substring match).
     */
    public List<InstagramDealItem> getDealsForGroup(String query, boolean onlyNew) {
        if (query == null) query = "";
        String search = query.toLowerCase().trim();

        List<InstagramDealItem> allItems = getAllInstagramItems();
        List<InstagramDealItem> matching = new ArrayList<>();

        for (InstagramDealItem item : allItems) {
            if (onlyNew && !"NEW".equalsIgnoreCase(item.getStatus()) && !"FAILED".equalsIgnoreCase(item.getStatus())) {
                continue;
            }

            String group = item.getGroup().toLowerCase();
            String title = item.getDeal().getTitle().toLowerCase();

            if (group.contains(search) || search.contains(group) 
                    || (search.contains("headset") && (group.contains("audio") || group.contains("earbud") || title.contains("headphone") || title.contains("earbud")))
                    || (search.contains("gadget") && (group.contains("gadget") || group.contains("tech")))
                    || (search.contains("watch") && (group.contains("watch") || title.contains("watch")))
                    || (search.contains("laptop") && (group.contains("laptop") || title.contains("laptop")))) {
                matching.add(item);
            }
        }
        return matching;
    }

    /**
     * Synchronizes and categorizes deals from the main sheet (Sheet1) into the Instagram sheet.
     */
    public int syncFromMainSheet(int limit) {
        ensureInstagramSheetExists();
        int addedCount = 0;

        try {
            ValueRange mainResponse = sheetsService.spreadsheets().values()
                    .get(spreadsheetId, mainSheetRange)
                    .execute();
            List<List<Object>> mainRows = mainResponse.getValues();

            if (mainRows == null || mainRows.isEmpty()) {
                return 0;
            }

            int targetLimit = Math.max(1, limit);

            for (List<Object> row : mainRows) {
                if (addedCount >= targetLimit) break;
                if (row == null || row.isEmpty()) continue;

                String title = row.size() > 0 ? row.get(0).toString().trim() : "";
                String price = row.size() > 1 ? row.get(1).toString().trim() : "";
                String image = row.size() > 2 ? row.get(2).toString().trim() : "";
                String link = row.size() > 3 ? row.get(3).toString().trim() : "";
                String source = row.size() > 4 ? row.get(4).toString().trim() : "Amazon";

                if (title.isEmpty() || title.equalsIgnoreCase("Title") || title.equalsIgnoreCase("Amazon Deal")
                        || price.isEmpty() || price.equalsIgnoreCase("N/A") || image.isEmpty() || link.isEmpty()) {
                    continue;
                }

                Deal deal = new Deal(title, price, image, link, source);
                String smartGroup = determineSmartGroup(deal);
                String targetType = determineTargetType(deal);

                boolean saved = saveInstagramDeal(deal, smartGroup, targetType);
                if (saved) {
                    addedCount++;
                }
            }
        } catch (Exception e) {
            System.err.println("Error syncing from main sheet to Instagram sheet: " + e.getMessage());
        }

        System.out.println("🔄 Synced " + addedCount + " deals to Instagram sheet.");
        return addedCount;
    }

    /**
     * Posts a grouped Instagram Carousel (e.g. for "Headsets" or "Best Gadgets") using items from the Instagram sheet.
     */
    public Map<String, Object> postGroupCarouselToInstagram(String groupName, int limit) {
        Map<String, Object> result = new HashMap<>();
        List<InstagramDealItem> items = getDealsForGroup(groupName, true);

        // Fallback: If no NEW items found, search among all items in that group
        if (items.isEmpty()) {
            items = getDealsForGroup(groupName, false);
        }

        if (items.isEmpty()) {
            result.put("status", "NO_DEALS_FOUND");
            result.put("message", "No deals found in Instagram sheet for group: " + groupName);
            return result;
        }

        int maxSlides = Math.min(Math.min(limit > 0 ? limit : 5, 10), items.size());
        List<InstagramDealItem> selectedItems = items.subList(0, maxSlides);

        List<Deal> deals = new ArrayList<>();
        for (InstagramDealItem item : selectedItems) {
            deals.add(item.getDeal());
        }

        // Build rich carousel caption
        String effectiveGroup = selectedItems.get(0).getGroup();
        String caption = buildGroupCarouselCaption(effectiveGroup, deals);

        boolean posted = instagramService.publishInstagramCarousel(deals, caption);

        if (posted) {
            for (InstagramDealItem item : selectedItems) {
                updateRowStatus(item.getRowNumber(), "POSTED");
            }
            result.put("status", "SUCCESS");
            result.put("group", effectiveGroup);
            result.put("slideCount", deals.size());
            result.put("message", "Successfully published " + deals.size() + "-slide Carousel for group '" + effectiveGroup + "' to Instagram!");
        } else {
            for (InstagramDealItem item : selectedItems) {
                updateRowStatus(item.getRowNumber(), "FAILED");
            }
            result.put("status", "FAILED");
            result.put("group", effectiveGroup);
            result.put("message", "Failed to publish Carousel to Instagram (check Meta Graph API credentials or image URLs).");
        }

        return result;
    }

    /**
     * Posts an Instagram Reel using the top-scored deal from a specific group in the Instagram sheet.
     */
    public Map<String, Object> postGroupReelToInstagram(String groupName) {
        Map<String, Object> result = new HashMap<>();
        List<InstagramDealItem> items = getDealsForGroup(groupName, true);

        if (items.isEmpty()) {
            items = getDealsForGroup(groupName, false);
        }

        if (items.isEmpty()) {
            result.put("status", "NO_DEALS_FOUND");
            result.put("message", "No deals found in Instagram sheet for group: " + groupName);
            return result;
        }

        // Score and pick highest-scoring deal in this group
        InstagramDealItem topItem = items.get(0);
        double maxScore = -1;

        for (InstagramDealItem item : items) {
            Deal d = item.getDeal();
            dealScoreService.scoreDeal(d);
            if (d.getDealScore() > maxScore) {
                maxScore = d.getDealScore();
                topItem = item;
            }
        }

        try {
            Deal deal = topItem.getDeal();
            ProductCategory cat = categoryService.detectCategory(deal.getTitle());
            com.example.telegram_bot.model.TrendingAudioTrack audio = trendingAudioService.getRecommendedTrack(deal, cat);

            videoGenerationService.createReel(deal);
            String videoUrl = serverBaseUrl + "/video/stream";

            boolean posted = instagramService.publishReel(deal, videoUrl);

            if (posted) {
                updateRowStatus(topItem.getRowNumber(), "POSTED");
                result.put("status", "SUCCESS");
                result.put("dealTitle", deal.getTitle());
                result.put("group", topItem.getGroup());
                result.put("dealScore", deal.getDealScore());
                if (audio != null) {
                    result.put("recommendedAudio", trendingAudioService.getTrackDetails(audio));
                }
                result.put("message", "Successfully published Reel for product '" + deal.getTitle() + "' to Instagram!");
            } else {
                updateRowStatus(topItem.getRowNumber(), "FAILED");
                result.put("status", "FAILED");
                if (audio != null) {
                    result.put("recommendedAudio", trendingAudioService.getTrackDetails(audio));
                }
                result.put("message", "Failed to publish Reel to Instagram.");
            }
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Updates the status column (Column G) for a specific row in the Instagram sheet.
     */
    public void updateRowStatus(int rowNumber, String status) {
        try {
            String updateRange = instagramSheetName + "!G" + rowNumber;
            ValueRange body = new ValueRange().setValues(Collections.singletonList(
                    Collections.singletonList(status)
            ));

            sheetsService.spreadsheets().values()
                    .update(spreadsheetId, updateRange, body)
                    .setValueInputOption("RAW")
                    .execute();

            System.out.println("Updated Instagram sheet row " + rowNumber + " -> Status: " + status);
        } catch (Exception e) {
            System.err.println("Error updating Instagram sheet status on row " + rowNumber + ": " + e.getMessage());
        }
    }

    /**
     * Builds an Instagram Carousel caption for a product group.
     */
    public String buildGroupCarouselCaption(String groupName, List<Deal> deals) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔥 TOP ").append(deals.size()).append(" ").append(groupName.toUpperCase()).append(" DEALS 🔥\n\n");
        sb.append("Swipe ➡️ to see all deals with huge price drops!\n\n");

        int index = 1;
        for (Deal deal : deals) {
            int discount = deal.calculateDiscountPercent();
            sb.append(index).append(". ").append(deal.getTitle()).append("\n");
            sb.append("💰 Deal Price: ₹").append(deal.getPrice());
            if (deal.getMrp() != null && !deal.getMrp().trim().isEmpty() && !deal.getMrp().equalsIgnoreCase("N/A")) {
                sb.append(" (MRP: ₹").append(deal.getMrp()).append(")");
            }
            if (discount > 0) {
                sb.append(" • ").append(discount).append("% OFF");
            }
            sb.append("\n\n");
            index++;
        }

        sb.append("👇 Comment \"LINK\" and we will DM you the direct purchase links for all items!\n\n");
        sb.append("❤️ Follow @offerzone2538 for daily curated top deals!\n\n");

        // Add dynamic product hashtags
        ProductCategory cat = categoryService.detectCategory(groupName);
        if (!deals.isEmpty()) {
            cat = categoryService.detectCategory(deals.get(0).getTitle());
        }
        String hashtags = hashtagService.getHashTags(cat, groupName);
        sb.append(hashtags);

        return sb.toString();
    }

    /**
     * Intelligently assigns a group name based on product title, category, discount, and price.
     */
    public String determineSmartGroup(Deal deal) {
        if (deal == null || deal.getTitle() == null) return "Best Gadgets";
        String titleLower = deal.getTitle().toLowerCase();

        // 1. Audio / Headsets
        if (titleLower.contains("earbuds") || titleLower.contains("tws") || titleLower.contains("airpods") 
                || titleLower.contains("headphone") || titleLower.contains("neckband") || titleLower.contains("airdopes")
                || titleLower.contains("speaker") || titleLower.contains("soundbar")) {
            return "Headsets & Audio";
        }

        // 2. Smartwatches
        if (titleLower.contains("watch") || titleLower.contains("smartwatch") || titleLower.contains("fitness band")) {
            return "Smartwatches";
        }

        // 3. Mobiles & Accessories
        if (titleLower.contains("iphone") || titleLower.contains("phone") || titleLower.contains("smartphone") 
                || titleLower.contains("galaxy") || titleLower.contains("oneplus") || titleLower.contains("5g")) {
            return "Top Smartphones";
        }

        // 4. Laptops & Tech
        if (titleLower.contains("laptop") || titleLower.contains("macbook") || titleLower.contains("notebook") 
                || titleLower.contains("gaming laptop") || titleLower.contains("ipad") || titleLower.contains("tablet")) {
            return "Laptops & Productivity";
        }

        // 5. Shoes & Footwear
        if (titleLower.contains("shoe") || titleLower.contains("sneaker") || titleLower.contains("running") || titleLower.contains("crocs")) {
            return "Trending Shoes & Kicks";
        }

        // 6. Kitchen Gadgets
        if (titleLower.contains("air fryer") || titleLower.contains("fryer") || titleLower.contains("mixer") 
                || titleLower.contains("grinder") || titleLower.contains("cooker") || titleLower.contains("kettle") || titleLower.contains("cookware")) {
            return "Smart Kitchen Finds";
        }

        // 7. Skincare & Grooming
        if (titleLower.contains("serum") || titleLower.contains("sunscreen") || titleLower.contains("trimmer") || titleLower.contains("shaver") || titleLower.contains("skincare")) {
            return "Grooming & Skincare";
        }

        // 8. Fitness
        if (titleLower.contains("protein") || titleLower.contains("whey") || titleLower.contains("creatine") || titleLower.contains("gym") || titleLower.contains("yoga")) {
            return "Fitness & Health";
        }

        // 9. Budget sweet spots
        try {
            if (deal.getPrice() != null) {
                double p = Double.parseDouble(deal.getPrice().replaceAll("[^0-9.]", ""));
                if (p > 0 && p <= 999) {
                    return "Budget Finds Under ₹999";
                }
            }
        } catch (Exception ignored) {}

        // 10. Default collection
        return "Best Gadgets";
    }

    private String determineTargetType(Deal deal) {
        int discount = deal.calculateDiscountPercent();
        if (discount >= 50) {
            return "REEL"; // High discounts make viral reels
        }
        return "CAROUSEL"; // Others fit carousel slides
    }

    private boolean isDuplicateInstagramDeal(Deal deal) {
        if (deal == null) return false;
        List<List<Object>> rows = getAllInstagramRows();
        if (rows.isEmpty()) return false;

        String newLink = deal.getLink() != null ? deal.getLink().trim() : "";
        String newTitle = deal.getTitle() != null ? deal.getTitle().trim() : "";

        for (List<Object> row : rows) {
            String existingTitle = row.size() > 0 ? row.get(0).toString().trim() : "";
            String existingLink = row.size() > 3 ? row.get(3).toString().trim() : "";

            if (!newLink.isEmpty() && newLink.equalsIgnoreCase(existingLink)) {
                return true;
            }
            if (!newTitle.isEmpty() && newTitle.equalsIgnoreCase(existingTitle)) {
                return true;
            }
        }
        return false;
    }
}
