package com.example.telegram_bot.service;

import org.springframework.stereotype.Service;

import com.example.telegram_bot.model.Deal;
import com.example.telegram_bot.model.ProductCategory;

@Service
public class MessageFormatterService {

    private final CategoryService categoryService;
    private final HashtagService hashtagService;

    public MessageFormatterService(CategoryService categoryService, HashtagService hashtagService) {
        this.categoryService = categoryService;
        this.hashtagService = hashtagService;
    }

    /**
     * Formats an attractive, high-converting HTML message for Telegram posts with deal rating badges and savings.
     */
    public String formatTelegramMessage(Deal deal) {
        ProductCategory category = categoryService.detectCategory(deal.getTitle());
        String categoryEmoji = getCategoryEmoji(category);
        String storeName = (deal.getSource() != null && !deal.getSource().trim().isEmpty()) ? deal.getSource() : "Amazon";
        int discount = deal.calculateDiscountPercent();
        String ratingBadge = deal.getDealRatingBadge();

        StringBuilder sb = new StringBuilder();
        sb.append(ratingBadge).append("\n\n");
        sb.append(categoryEmoji).append(" <b>").append(escapeHtml(deal.getTitle())).append("</b>\n\n");
        sb.append("💰 <b>Special Price:</b> ₹<b>").append(deal.getPrice()).append("</b>");

        if (deal.getMrp() != null && !deal.getMrp().isEmpty() && !deal.getMrp().equalsIgnoreCase("N/A")) {
            sb.append(" <s>(MRP: ₹").append(deal.getMrp()).append(")</s>");
        }
        sb.append("\n");

        if (discount > 0) {
            sb.append("⚡ <b>Discount:</b> <b>").append(discount).append("% OFF</b>\n");
        }

        sb.append("🏷️ <b>Store:</b> ").append(storeName).append("\n\n");
        sb.append("⚡ <i>Limited time offer — prices may change quickly!</i>\n\n");
        sb.append("👇 <b>Tap below to Buy Now:</b>");

        return sb.toString();
    }

    /**
     * Formats a product-tailored caption with deal rating badges, discount %, and hashtags for Instagram.
     */
    public String formatInstagramCaption(Deal deal) {
        ProductCategory category = categoryService.detectCategory(deal.getTitle());
        String hashtags = hashtagService.getHashTags(category);
        String categoryEmoji = getCategoryEmoji(category);
        int discount = deal.calculateDiscountPercent();
        String ratingBadge = deal.getDealRatingBadge();

        StringBuilder sb = new StringBuilder();
        sb.append(ratingBadge).append("\n\n");
        sb.append(categoryEmoji).append(" ").append(deal.getTitle()).append("\n\n");
        sb.append("💰 Special Offer: ₹").append(deal.getPrice());

        if (deal.getMrp() != null && !deal.getMrp().isEmpty() && !deal.getMrp().equalsIgnoreCase("N/A")) {
            sb.append(" (MRP: ₹").append(deal.getMrp()).append(")");
        }
        sb.append("\n");

        if (discount > 0) {
            sb.append("⚡ Save ").append(discount).append("% Today!\n");
        }
        sb.append("\n");

        sb.append("👇 Comment \"LINK\" and we will DM you the direct purchase link!\n\n");
        sb.append("❤️ Follow @offerzone2538 for daily top deals & savings.\n\n");
        sb.append(hashtags);

        return sb.toString();
    }

    public String getCategoryEmoji(ProductCategory category) {
        if (category == null) return "🛒";
        switch (category) {
            case WATCH: return "⌚";
            case MOBILE: return "📱";
            case LAPTOP: return "💻";
            case TV: return "📺";
            case HEADPHONE: return "🎧";
            case SHOE: return "👟";
            case SHIRT: return "👔";
            case DRESS: return "👗";
            case KITCHEN: return "🍳";
            case HOME: return "🏠";
            case BEAUTY: return "💄";
            case BOOK: return "📚";
            case TOY: return "🧸";
            default: return "🛒";
        }
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
