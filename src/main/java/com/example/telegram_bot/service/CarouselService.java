package com.example.telegram_bot.service;

import com.example.telegram_bot.model.Deal;
import com.example.telegram_bot.model.ProductCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CarouselService {

    private final GoogleSheetService googleSheetService;
    private final CategoryService categoryService;
    private final HashtagService hashtagService;
    private final InstagramService instagramService;

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
                    || (search.contains("bluetooth") && entry.getKey() == ProductCategory.HEADPHONE)) {
                matchingDeals.addAll(entry.getValue());
            }
        }
        return matchingDeals;
    }

    /**
     * Formats a rich carousel summary text for a list of grouped deals.
     */
    public String buildCarouselCaption(ProductCategory category, List<Deal> deals) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔥 <b>TOP ").append(deals.size()).append(" ").append(category.name()).append(" DEALS CAROUSEL</b> 🔥\n\n");

        int index = 1;
        for (Deal deal : deals) {
            sb.append("<b>").append(index).append(". ").append(deal.getTitle()).append("</b>\n");
            sb.append("💰 Price: ₹").append(deal.getPrice()).append("\n");
            sb.append("🔗 Link: <a href=\"").append(deal.getLink()).append("\">Buy Here</a>\n\n");
            index++;
            if (index > 10) break;
        }

        String hashtags = hashtagService.getHashTags(category);
        sb.append(hashtags);

        return sb.toString();
    }

    /**
     * Posts a grouped multi-item product carousel exclusively to Instagram.
     */
    public boolean postCategoryCarouselToInstagram(String categoryQuery) throws Exception {
        List<Deal> deals = getDealsForCategoryName(categoryQuery);
        if (deals == null || deals.isEmpty()) {
            System.out.println("No deals found for category query: " + categoryQuery);
            return false;
        }

        ProductCategory category = categoryService.detectCategory(categoryQuery);
        String caption = buildCarouselCaption(category, deals);

        return instagramService.publishInstagramCarousel(deals, caption);
    }
}
