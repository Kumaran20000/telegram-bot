package com.example.telegram_bot.service;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.telegram_bot.model.ProductCategory;

class HashtagServiceTest {

    private HashtagService hashtagService;
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        hashtagService = new HashtagService();
        categoryService = new CategoryService();
    }

    @Test
    void testBoatEarbudsHashtags() {
        String title = "boAt Airdopes 141 Bluetooth Truly Wireless in Ear Earbuds with 42H Playtime";
        ProductCategory category = categoryService.detectCategory(title);

        String instagramTags = hashtagService.getHashTags(category, title, "999", "4490", "78% off");
        assertNotNull(instagramTags);
        assertTrue(instagramTags.contains("#boat"), "Should contain brand tag #boat");
        assertTrue(instagramTags.contains("#earbuds"), "Should contain #earbuds");
        assertTrue(instagramTags.contains("#tws"), "Should contain #tws");
        assertTrue(instagramTags.contains("#stealdeal") || instagramTags.contains("#lootdeal"), "Should contain loot/steal deal tag");
        assertTrue(instagramTags.contains("#under1000"), "Should contain #under1000 tag");

        String telegramTags = hashtagService.getTelegramHashTags(category, title);
        assertTrue(telegramTags.contains("#boat"));
        assertTrue(telegramTags.contains("#AmazonDeals"));
    }

    @Test
    void testIphoneHashtags() {
        String title = "Apple iPhone 15 (128 GB) - Black";
        ProductCategory category = categoryService.detectCategory(title);

        String tags = hashtagService.getHashTags(category, title, "69999", "79900", "12% off");
        assertTrue(tags.contains("#apple"), "Should contain #apple");
        assertTrue(tags.contains("#iphone"), "Should contain #iphone");
        assertTrue(tags.contains("#smartphone"), "Should contain #smartphone");
    }

    @Test
    void testAirFryerHashtags() {
        String title = "Pigeon Healthifry Digital Air Fryer 4.2 Litre 1200W";
        ProductCategory category = categoryService.detectCategory(title);

        String tags = hashtagService.getHashTags(category, title, "2499", "5995", "58% off");
        assertTrue(tags.contains("#pigeon"), "Should contain #pigeon");
        assertTrue(tags.contains("#airfryer"), "Should contain #airfryer");
        assertTrue(tags.contains("#kitchen"), "Should contain #kitchen or kitchen gadgets");
    }

    @Test
    void testShoesHashtags() {
        String title = "Puma Mens Nitro Running Shoes";
        ProductCategory category = categoryService.detectCategory(title);

        String tags = hashtagService.getHashTags(category, title, "3499", "7999", "56% off");
        assertTrue(tags.contains("#puma"), "Should contain #puma");
        assertTrue(tags.contains("#shoes") || tags.contains("#sneakers"), "Should contain #shoes or #sneakers");
        assertTrue(tags.contains("#footwear"), "Should contain #footwear");
    }

    @Test
    void testSkincareSerumHashtags() {
        String title = "Minimalist 10% Vitamin C Face Serum with Centella Water for Glowing Skin";
        ProductCategory category = categoryService.detectCategory(title);

        String tags = hashtagService.getHashTags(category, title, "499", "699", "28% off");
        assertTrue(tags.contains("#minimalist"), "Should contain #minimalist");
        assertTrue(tags.contains("#skincare"), "Should contain #skincare");
        assertTrue(tags.contains("#faceserum"), "Should contain #faceserum");
        assertTrue(tags.contains("#under500"), "Should contain #under500");
    }

    @Test
    void testWheyProteinHashtags() {
        String title = "MuscleBlaze Biozyme Performance Whey Protein Powder 2kg";
        ProductCategory category = categoryService.detectCategory(title);

        String tags = hashtagService.getHashTags(category, title, "4299", "5599", "23% off");
        assertTrue(tags.contains("#muscleblaze"), "Should contain #muscleblaze");
        assertTrue(tags.contains("#wheyprotein"), "Should contain #wheyprotein");
        assertTrue(tags.contains("#fitness"), "Should contain #fitness");
    }

    @Test
    void testGenerateProductHashtagDetails() {
        String title = "Noise ColorFit Pulse 2 Max 1.85 Display Bluetooth Calling Smart Watch";
        ProductCategory category = categoryService.detectCategory(title);

        Map<String, Object> details = hashtagService.generateProductHashtagDetails(
                title, "1499", "5999", "75% off", category);

        assertNotNull(details);
        assertTrue(details.containsKey("brandTags"));
        assertTrue(details.containsKey("instagramHashtags"));
        assertTrue(details.containsKey("telegramHashtags"));
        assertTrue(details.containsKey("facebookHashtags"));

        String ig = (String) details.get("instagramHashtags");
        assertTrue(ig.contains("#noise"));
        assertTrue(ig.contains("#smartwatch"));
    }
}
