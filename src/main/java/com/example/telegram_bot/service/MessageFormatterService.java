package com.example.telegram_bot.service;

import java.util.concurrent.ThreadLocalRandom;

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
     * Formats a Telegram post choosing randomly among 10 distinct high-converting content templates.
     */
    public String formatTelegramMessage(Deal deal) {
        int index = ThreadLocalRandom.current().nextInt(10);
        return formatTelegramMessage(deal, index);
    }

    /**
     * Formats a Telegram message using a specific template index (0 to 9).
     */
    public String formatTelegramMessage(Deal deal, int templateIndex) {
        ProductCategory category = categoryService.detectCategory(deal.getTitle());
        String categoryEmoji = getCategoryEmoji(category);
        String storeName = (deal.getSource() != null && !deal.getSource().trim().isEmpty()) ? deal.getSource() : "Amazon";
        int discount = deal.calculateDiscountPercent();
        String ratingBadge = deal.getDealRatingBadge();
        String cleanTitle = escapeHtml(deal.getTitle());
        String price = deal.getPrice() != null ? deal.getPrice() : "N/A";
        String mrp = deal.getMrp();
        boolean hasMrp = mrp != null && !mrp.isEmpty() && !mrp.equalsIgnoreCase("N/A");

        int variant = Math.abs(templateIndex) % 10;
        StringBuilder sb = new StringBuilder();

        switch (variant) {
            case 0:
                // Classic High-Energy
                sb.append(ratingBadge).append("\n\n");
                sb.append(categoryEmoji).append(" <b>").append(cleanTitle).append("</b>\n\n");
                sb.append("💰 <b>Special Price:</b> ₹<b>").append(price).append("</b>");
                if (hasMrp) sb.append(" <s>(MRP: ₹").append(mrp).append(")</s>");
                sb.append("\n");
                if (discount > 0) sb.append("⚡ <b>Discount:</b> <b>").append(discount).append("% OFF</b>\n");
                sb.append("🏷️ <b>Store:</b> ").append(storeName).append("\n\n");
                sb.append("⚡ <i>Limited time offer — prices may change quickly!</i>\n\n");
                sb.append("👇 <b>Tap below to Buy Now:</b>");
                break;

            case 1:
                // Minimalist & Direct
                sb.append("💥 <b>PRICE DROP ALERT!</b> 📉\n\n");
                sb.append(categoryEmoji).append(" <b>").append(cleanTitle).append("</b>\n\n");
                sb.append("💵 <b>Offer Price:</b> ₹<b>").append(price).append("</b>\n");
                if (hasMrp) sb.append("❌ <b>Original Price:</b> <s>₹").append(mrp).append("</s>\n");
                if (discount > 0) sb.append("🎉 <b>You Save:</b> <b>").append(discount).append("% OFF!</b>\n");
                sb.append("🛒 <b>Available on:</b> ").append(storeName).append("\n\n");
                sb.append("👇 <b>Grab this deal before stock runs out:</b>");
                break;

            case 2:
                // Loot/Steal Deal Style
                sb.append("💎 <b>STEAL DEAL OF THE DAY!</b> 💎\n\n");
                sb.append(categoryEmoji).append(" <b>").append(cleanTitle).append("</b>\n\n");
                sb.append("🔥 <b>Deal Price:</b> ₹<b>").append(price).append(" ONLY!</b>\n");
                if (discount > 0) sb.append("⚡ <b>Flat ").append(discount).append("% Discount Applied!</b>\n");
                if (hasMrp) sb.append("📌 <s>MRP: ₹").append(mrp).append("</s>\n");
                sb.append("📍 <b>Store:</b> ").append(storeName).append("\n\n");
                sb.append("⏰ <i>Hurry! Price can rise anytime!</i>\n\n");
                sb.append("👇 <b>Tap link below to order now:</b>");
                break;

            case 3:
                // Question / Engagement Hook
                sb.append("👀 <b>Looking for a huge price drop? Check this out!</b>\n\n");
                sb.append(categoryEmoji).append(" <b>").append(cleanTitle).append("</b>\n\n");
                sb.append("🏷️ <b>Deal Price:</b> ₹<b>").append(price).append("</b>");
                if (hasMrp) sb.append(" <s>₹").append(mrp).append("</s>");
                sb.append("\n");
                if (discount > 0) sb.append("⚡ <b>Huge ").append(discount).append("% OFF Today!</b>\n");
                sb.append("🛒 <b>Merchant:</b> ").append(storeName).append("\n\n");
                sb.append("👇 <b>Click below to buy now:</b>");
                break;

            case 4:
                // Flash Sale Countdown
                sb.append("⏰ <b>FLASH SALE IS LIVE!</b> ⚡\n\n");
                sb.append(categoryEmoji).append(" <b>").append(cleanTitle).append("</b>\n\n");
                sb.append("💰 <b>Today's Special:</b> ₹<b>").append(price).append("</b>");
                if (hasMrp) sb.append(" <s>(MRP ₹").append(mrp).append(")</s>");
                sb.append("\n");
                if (discount > 0) sb.append("💥 <b>Savings:</b> <b>").append(discount).append("% OFF!</b>\n");
                sb.append("📦 <b>Verified Offer on:</b> ").append(storeName).append("\n\n");
                sb.append("👇 <b>Tap the button below to buy immediately:</b>");
                break;

            case 5:
                // Bulleted Feature Highlight
                sb.append("🚨 <b>HUGE SAVINGS ALERT</b> 🚨\n\n");
                sb.append("📦 <b>Product:</b> ").append(cleanTitle).append("\n");
                sb.append("💰 <b>Offer Price:</b> ₹<b>").append(price).append("</b>\n");
                if (discount > 0) sb.append("🔥 <b>Savings:</b> <b>").append(discount).append("% OFF</b>");
                if (hasMrp) sb.append(" <s>(MRP: ₹").append(mrp).append(")</s>");
                sb.append("\n");
                sb.append("🏷️ <b>Store:</b> ").append(storeName).append("\n\n");
                sb.append("✨ <i>Top deal with fast delivery!</i>\n\n");
                sb.append("👇 <b>Tap below to get direct discount link:</b>");
                break;

            case 6:
                // Urgency / Stock Warning
                sb.append("⚡ <b>LIMITED STOCK DEAL!</b> 🏃‍♂️💨\n\n");
                sb.append(categoryEmoji).append(" <b>").append(cleanTitle).append("</b>\n\n");
                sb.append("💵 <b>Special Rate:</b> ₹<b>").append(price).append("</b>\n");
                if (discount > 0) sb.append("🔥 <b>Discount:</b> <b>").append(discount).append("% OFF</b>");
                if (hasMrp) sb.append(" <s>(MRP ₹").append(mrp).append(")</s>");
                sb.append("\n");
                sb.append("🛒 <b>Store:</b> ").append(storeName).append("\n\n");
                sb.append("⏳ <i>Deal may expire soon!</i>\n\n");
                sb.append("👇 <b>Click below to order yours:</b>");
                break;

            case 7:
                // Today's Best Offer
                sb.append("🌟 <b>TODAY'S BEST OFFER</b> 🌟\n\n");
                sb.append(categoryEmoji).append(" <b>").append(cleanTitle).append("</b>\n\n");
                sb.append("💰 <b>Grab it for ₹").append(price).append("</b>");
                if (hasMrp) sb.append(" <s>(MRP ₹").append(mrp).append(")</s>");
                sb.append("\n");
                if (discount > 0) sb.append("💥 <b>Discount:</b> <b>").append(discount).append("% OFF</b>\n");
                sb.append("🏷️ <b>Source:</b> ").append(storeName).append("\n\n");
                sb.append("🔥 <i>Don't miss out on this deal!</i>\n\n");
                sb.append("👇 <b>Tap button below to buy now:</b>");
                break;

            case 8:
                // Short & Punchy
                sb.append("🔥 <b>MEGA DISCOUNT DETECTED!</b> 🎯\n\n");
                sb.append(categoryEmoji).append(" <b>").append(cleanTitle).append("</b>\n\n");
                sb.append("💲 <b>Price:</b> ₹<b>").append(price).append("</b>");
                if (hasMrp) sb.append(" <s>(MRP ₹").append(mrp).append(")</s>");
                sb.append("\n");
                if (discount > 0) sb.append("⚡ <b>").append(discount).append("% OFF Right Now!</b>\n");
                sb.append("🛒 <b>Store:</b> ").append(storeName).append("\n\n");
                sb.append("👇 <b>Click below to shop now:</b>");
                break;

            case 9:
            default:
                // Exclusive Bargain
                sb.append("🎉 <b>BEST BARGAIN FIND!</b> 🛍️\n\n");
                sb.append(categoryEmoji).append(" <b>").append(cleanTitle).append("</b>\n\n");
                sb.append("💰 <b>Price Today:</b> ₹<b>").append(price).append("</b>\n");
                if (discount > 0) sb.append("💥 <b>Save ").append(discount).append("%</b>");
                if (hasMrp) sb.append(" <s>(MRP ₹").append(mrp).append(")</s>");
                sb.append("\n");
                sb.append("🏷️ <b>Sold via:</b> ").append(storeName).append("\n\n");
                sb.append("👇 <b>Tap below to claim offer:</b>");
                break;
        }

        return sb.toString();
    }

    /**
     * Formats a post specifically tailored for a Facebook Page.
     */
    public String formatFacebookPost(Deal deal) {
        ProductCategory category = categoryService.detectCategory(deal.getTitle());
        String hashtags = hashtagService.getHashTags(category);
        String categoryEmoji = getCategoryEmoji(category);
        String storeName = (deal.getSource() != null && !deal.getSource().trim().isEmpty()) ? deal.getSource() : "Amazon";
        int discount = deal.calculateDiscountPercent();
        String ratingBadge = deal.getDealRatingBadge();
        String title = deal.getTitle();
        String price = deal.getPrice() != null ? deal.getPrice() : "N/A";
        String mrp = deal.getMrp();
        boolean hasMrp = mrp != null && !mrp.isEmpty() && !mrp.equalsIgnoreCase("N/A");

        StringBuilder sb = new StringBuilder();
        sb.append(ratingBadge).append("\n\n");
        sb.append(categoryEmoji).append(" ").append(title).append("\n\n");
        sb.append("💰 Deal Price: ₹").append(price);
        if (hasMrp) sb.append(" (MRP: ₹").append(mrp).append(")");
        sb.append("\n");
        if (discount > 0) sb.append("⚡ Discount: ").append(discount).append("% OFF\n");
        sb.append("🏷️ Store: ").append(storeName).append("\n\n");
        if (deal.getLink() != null && !deal.getLink().trim().isEmpty()) {
            sb.append("👉 Buy Now on ").append(storeName).append(": ").append(deal.getLink()).append("\n\n");
        }
        sb.append("⚡ Limited time offer — prices may change quickly!\n\n");
        sb.append(hashtags);
        return sb.toString();
    }

    /**
     * Formats an Instagram caption choosing randomly among 10 distinct content templates.
     */
    public String formatInstagramCaption(Deal deal) {
        int index = ThreadLocalRandom.current().nextInt(10);
        return formatInstagramCaption(deal, index);
    }

    /**
     * Formats an Instagram caption using a specific template index (0 to 9).
     */
    public String formatInstagramCaption(Deal deal, int templateIndex) {
        ProductCategory category = categoryService.detectCategory(deal.getTitle());
        String hashtags = hashtagService.getHashTags(category, deal.getTitle());
        String categoryEmoji = getCategoryEmoji(category);
        int discount = deal.calculateDiscountPercent();
        long savings = deal.calculateSavingsAmount();
        String ratingBadge = deal.getDealRatingBadge();
        String title = deal.getTitle();
        String price = deal.getPrice() != null ? deal.getPrice() : "N/A";
        String mrp = deal.getMrp();
        boolean hasMrp = mrp != null && !mrp.isEmpty() && !mrp.equalsIgnoreCase("N/A");
        String savingsText = savings > 0 ? " (Save ₹" + String.format("%,d", savings) + ")" : "";

        int variant = Math.abs(templateIndex) % 10;
        StringBuilder sb = new StringBuilder();

        switch (variant) {
            case 0:
                // Comment "LINK" Classic
                sb.append(ratingBadge).append("\n\n");
                sb.append(categoryEmoji).append(" ").append(title).append("\n\n");
                sb.append("🔥 Offer Price: ₹").append(price).append("\n");
                if (hasMrp) sb.append("❌ Original MRP: ₹").append(mrp).append("\n");
                if (discount > 0) sb.append("💥 Discount: ").append(discount).append("% OFF").append(savingsText).append("\n");
                sb.append("\n");
                sb.append("👇 Comment \"LINK\" and we will DM you the direct purchase link!\n\n");
                sb.append("❤️ Follow @offerzone2538 for daily top deals & savings.\n\n");
                sb.append(hashtags);
                break;

            case 1:
                // Comment "DEAL" / Inbox Hook
                sb.append("💥 PRICE DROP ALERT! 📉\n\n");
                sb.append(categoryEmoji).append(" ").append(title).append("\n\n");
                sb.append("💰 Deal Price: ₹").append(price).append(" ONLY\n");
                if (hasMrp) sb.append("📌 Original List Price: ₹").append(mrp).append("\n");
                if (discount > 0) sb.append("⚡ Flat ").append(discount).append("% OFF").append(savingsText).append("!\n");
                sb.append("\n");
                sb.append("👇 Drop a comment saying \"DEAL\" and check your DM for the link! 📩\n\n");
                sb.append("🔥 Follow @offerzone2538 to never miss a secret deal!\n\n");
                sb.append(hashtags);
                break;

            case 2:
                // Steal Deal / Bio Link Hook
                sb.append("💎 STEAL DEAL OF THE DAY 💎\n\n");
                sb.append(categoryEmoji).append(" ").append(title).append("\n\n");
                sb.append("💵 Special Offer Rate: ₹").append(price).append("\n");
                if (hasMrp) sb.append("❌ Regular MRP: ₹").append(mrp).append("\n");
                if (discount > 0) sb.append("💥 Savings: ").append(discount).append("% OFF").append(savingsText).append("!\n");
                sb.append("\n");
                sb.append("👇 Want this deal? Comment \"WANT\" or check link in BIO!\n\n");
                sb.append("✨ Double tap ❤️ if you love savings! Follow @offerzone2538 for more!\n\n");
                sb.append(hashtags);
                break;

            case 3:
                // Limited Stock Hook
                sb.append("🚨 HOT PRODUCT ON DISCOUNT! 🚨\n\n");
                sb.append(categoryEmoji).append(" ").append(title).append("\n\n");
                sb.append("💰 Offer Price: ₹").append(price).append("\n");
                if (hasMrp) sb.append("❌ MRP: ₹").append(mrp).append("\n");
                if (discount > 0) sb.append("⚡ Discount: ").append(discount).append("% OFF").append(savingsText).append("\n");
                sb.append("\n");
                sb.append("👇 Comment \"BUY\" and we'll send the direct link to your inbox! 📬\n\n");
                sb.append("⏰ Prices change fast, grab it before it's gone!\n");
                sb.append("❤️ Follow @offerzone2538 for daily savings.\n\n");
                sb.append(hashtags);
                break;

            case 4:
                // Flash Sale Hook
                sb.append("⏰ FLASH SALE ALERT! ⚡\n\n");
                sb.append(categoryEmoji).append(" ").append(title).append("\n\n");
                sb.append("🔥 Offer Price Today: ₹").append(price).append("\n");
                if (hasMrp) sb.append("📌 MRP Price: ₹").append(mrp).append("\n");
                if (discount > 0) sb.append("⚡ Massive ").append(discount).append("% Discount").append(savingsText).append("!\n");
                sb.append("\n");
                sb.append("👇 Comment \"SEND LINK\" below and get it sent straight to your DM!\n\n");
                sb.append("📱 Turn on post notifications so you never miss a price drop!\n");
                sb.append("❤️ Follow @offerzone2538\n\n");
                sb.append(hashtags);
                break;

            case 5:
                // Loot Offer Hook
                sb.append("🎉 TODAY'S TOP PICK! 🛍️\n\n");
                sb.append(categoryEmoji).append(" ").append(title).append("\n\n");
                sb.append("💰 Offer Price: ₹").append(price).append("\n");
                if (hasMrp) sb.append("❌ List Price: ₹").append(mrp).append("\n");
                if (discount > 0) sb.append("🔥 Savings: ").append(discount).append("% OFF").append(savingsText).append("\n");
                sb.append("\n");
                sb.append("👇 Comment \"GET\" to receive the instant purchase link!\n\n");
                sb.append("💥 Tag a friend who needs this!\n");
                sb.append("❤️ Follow @offerzone2538\n\n");
                sb.append(hashtags);
                break;

            case 6:
                // Review / Rating Style
                sb.append("🌟 HIGHLY RATED DEAL 🌟\n\n");
                sb.append(categoryEmoji).append(" ").append(title).append("\n\n");
                sb.append("💰 Special Offer: ₹").append(price).append("\n");
                if (hasMrp) sb.append("📌 Original Price: ₹").append(mrp).append("\n");
                if (discount > 0) sb.append("⚡ Discount: ").append(discount).append("% Instant OFF").append(savingsText).append("!\n");
                sb.append("\n");
                sb.append("👇 Type \"LINK\" in the comments for instant link in DM! 📥\n\n");
                sb.append("❤️ Follow @offerzone2538 for curated top Amazon deals daily!\n\n");
                sb.append(hashtags);
                break;

            case 7:
                // Bargain Hunters Hook
                sb.append("🎯 BARGAIN HUNTERS SPECIAL! 🎯\n\n");
                sb.append(categoryEmoji).append(" ").append(title).append("\n\n");
                sb.append("💵 Deal Price: ₹").append(price).append("\n");
                if (hasMrp) sb.append("❌ MRP: ₹").append(mrp).append("\n");
                if (discount > 0) sb.append("⚡ Total Discount: ").append(discount).append("% OFF").append(savingsText).append("\n");
                sb.append("\n");
                sb.append("👇 Comment \"YES\" and we'll DM you the link right away!\n\n");
                sb.append("📌 Save this post for later!\n");
                sb.append("❤️ Follow @offerzone2538 for daily shopping deals!\n\n");
                sb.append(hashtags);
                break;

            case 8:
                // Short & Viral
                sb.append("🔥 MEGA DISCOUNT UNLOCKED! 🔥\n\n");
                sb.append(categoryEmoji).append(" ").append(title).append("\n\n");
                sb.append("💲 Special Offer: ₹").append(price).append("\n");
                if (hasMrp) sb.append("📌 Original MRP: ₹").append(mrp).append("\n");
                if (discount > 0) sb.append("💥 Discount: ").append(discount).append("% OFF").append(savingsText).append("!\n");
                sb.append("\n");
                sb.append("👇 Comment \"LINK\" for direct store link!\n\n");
                sb.append("❤️ Follow @offerzone2538 for non-stop deal alerts!\n\n");
                sb.append(hashtags);
                break;

            case 9:
            default:
                // Shopping Guide / Deal of the Day
                sb.append("🛍️ DEAL OF THE DAY! 🛍️\n\n");
                sb.append(categoryEmoji).append(" ").append(title).append("\n\n");
                sb.append("💰 Today's Offer Price: ₹").append(price).append("\n");
                if (hasMrp) sb.append("❌ Original List MRP: ₹").append(mrp).append("\n");
                if (discount > 0) sb.append("⚡ Save ").append(discount).append("% OFF").append(savingsText).append("!\n");
                sb.append("\n");
                sb.append("👇 Comment \"SHOP\" and we will send the purchase link to your DMs!\n\n");
                sb.append("🔥 Follow @offerzone2538 for the best deals every day!\n\n");
                sb.append(hashtags);
                break;
        }

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

