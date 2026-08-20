package com.example.telegram_bot.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.telegram_bot.model.Deal;
import com.example.telegram_bot.model.ProductCategory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CarouselService {

    private final GoogleSheetService googleSheetService;
    private final CategoryService categoryService;
    private final HashtagService hashtagService;
    private final InstagramService instagramService;
    private final TelegramService telegramService;
    private final MessageFormatterService messageFormatterService;

    /**
     * Groups all deals in Google Sheet by ProductCategory.
     */
    public Map<ProductCategory, List<Deal>> groupDealsByCategory() throws Exception {
        List<List<Object>> rows = googleSheetService.getAllRows();
        Map<ProductCategory, List<Deal>> groupedMap = new LinkedHashMap<>();

        if (rows == null || rows.isEmpty()) {
            return groupedMap;
        }

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

            ProductCategory category = categoryService.detectCategory(title);
            groupedMap.computeIfAbsent(category, k -> new ArrayList<>()).add(deal);
        }

        return groupedMap;
    }

    /**
     * Finds all deals for a specific category string (e.g. "bluetooth", "watch", "laptop", "shoe").
     */
    public List<Deal> getDealsForCategoryName(String query) throws Exception {
        Map<ProductCategory, List<Deal>> grouped = groupDealsByCategory();
        String search = query != null ? query.toLowerCase().trim() : "";

        List<Deal> matchingDeals = new ArrayList<>();
        for (Map.Entry<ProductCategory, List<Deal>> entry : grouped.entrySet()) {
            String categoryStr = entry.getKey().name().toLowerCase();
            if (categoryStr.contains(search) || search.contains(categoryStr)
                    || (search.contains("bluetooth") && entry.getKey() == ProductCategory.HEADPHONE)
                    || (search.contains("phone") && entry.getKey() == ProductCategory.MOBILE)
                    || (search.contains("sneaker") && entry.getKey() == ProductCategory.SHOE)) {
                matchingDeals.addAll(entry.getValue());
            }
        }
        return matchingDeals;
    }

    /**
     * Accumulates 3 to 5 deals for a requested product category (e.g. LAPTOP, SHOE, WATCH, PHONE)
     * and generates a formatted DM message with direct purchase links ready for Instagram/Telegram DMs.
     */
    public Map<String, Object> getCategoryDmContent(String categoryQuery, int limit) throws Exception {
        List<Deal> deals = getDealsForCategoryName(categoryQuery);
        Map<String, Object> result = new HashMap<>();

        if (deals == null || deals.isEmpty()) {
            result.put("status", "NO_DEALS_FOUND");
            result.put("category", categoryQuery);
            result.put("count", 0);
            result.put("dmText", "Sorry, no active deals found right now for " + categoryQuery.toUpperCase() + ". Check back soon! 🔥");
            result.put("deals", Collections.emptyList());
            return result;
        }

        int maxCount = Math.min(Math.max(limit, 1), deals.size());
        List<Deal> accumulatedDeals = deals.subList(0, maxCount);

        ProductCategory detectedCat = categoryService.detectCategory(categoryQuery);
        String categoryEmoji = messageFormatterService.getCategoryEmoji(detectedCat);
        String catName = detectedCat != ProductCategory.DEFAULT ? detectedCat.name() : categoryQuery.toUpperCase();

        StringBuilder sb = new StringBuilder();
        sb.append(categoryEmoji).append(" <b>TOP ").append(accumulatedDeals.size())
          .append(" ").append(catName).append(" DEALS FOR YOU!</b> ").append(categoryEmoji).append("\n\n");

        int index = 1;
        for (Deal deal : accumulatedDeals) {
            int discount = deal.calculateDiscountPercent();
            sb.append("<b>").append(index).append(". ").append(deal.getTitle()).append("</b>\n");
            sb.append("💰 Price: ₹<b>").append(deal.getPrice()).append("</b>");
            if (discount > 0) {
                sb.append(" (<b>").append(discount).append("% OFF</b>)");
            }
            sb.append("\n🛒 Link: ").append(deal.getLink()).append("\n\n");
            index++;
        }

        sb.append("💡 <i>Comment another product (e.g., LAPTOP, SHOE, WATCH, PHONE) to get more deal links!</i>\n");
        sb.append("❤️ Follow @offerzone2538 for daily top deal alerts!");

        result.put("status", "SUCCESS");
        result.put("category", catName);
        result.put("count", accumulatedDeals.size());
        result.put("dmText", sb.toString());
        result.put("deals", accumulatedDeals);
        return result;
    }

    /**
     * Formats a rich carousel summary text for a list of grouped deals.
     */
    public String buildCarouselCaption(ProductCategory category, List<Deal> deals) {
        StringBuilder sb = new StringBuilder();
        String catName = (category != null && category != ProductCategory.DEFAULT) ? category.name() : "AMAZON";
        sb.append("🔥 TOP ").append(deals.size()).append(" ").append(catName).append(" DEALS 🔥\n\n");
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
            if (index > 10) break;
        }

        sb.append("👇 Comment \"LINK\" and we will DM you the direct purchase links!\n\n");
        sb.append("❤️ Follow @offerzone2538 for daily curated top deals!\n\n");

        String hashtags = hashtagService.getHashTags(category, !deals.isEmpty() ? deals.get(0).getTitle() : "");
        sb.append(hashtags);

        return sb.toString();
    }

    /**
     * Posts a grouped multi-item product carousel to Instagram (up to limit, default 5).
     */
    public boolean postCategoryCarouselToInstagram(String categoryQuery) throws Exception {
        return postCategoryCarouselToInstagram(categoryQuery, 5);
    }

    public boolean postCategoryCarouselToInstagram(String categoryQuery, int limit) throws Exception {
        List<Deal> deals = getDealsForCategoryName(categoryQuery);
        if (deals == null || deals.isEmpty()) {
            System.out.println("No deals found for category query: " + categoryQuery);
            return false;
        }

        int maxCount = Math.min(Math.max(limit, 1), deals.size());
        List<Deal> accumulatedDeals = deals.subList(0, maxCount);

        ProductCategory category = categoryService.detectCategory(categoryQuery);
        String caption = buildCarouselCaption(category, accumulatedDeals);

        return instagramService.publishInstagramCarousel(accumulatedDeals, caption);
    }

    /**
     * Posts an accumulated category deals group message (3-5 items) to Telegram.
     */
    public boolean postCategoryGroupToTelegram(String categoryQuery, int limit) throws Exception {
        Map<String, Object> content = getCategoryDmContent(categoryQuery, limit);
        if (!"SUCCESS".equals(content.get("status"))) {
            return false;
        }
        String messageHtml = (String) content.get("dmText");
        @SuppressWarnings("unchecked")
        List<Deal> deals = (List<Deal>) content.get("deals");

        boolean mediaSent = telegramService.sendMediaGroup(deals, messageHtml);
        if (!mediaSent) {
            return telegramService.sendMessageWithButton(messageHtml, "🛒 View All Deals", "https://t.me/BOnlinediscount");
        }
        return true;
    }
}

