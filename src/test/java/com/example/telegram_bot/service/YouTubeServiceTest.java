package com.example.telegram_bot.service;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.telegram_bot.model.Deal;
import com.example.telegram_bot.model.ProductCategory;

class YouTubeServiceTest {

    private YouTubeService youtubeService;

    @BeforeEach
    void setUp() {
        CategoryService categoryService = new CategoryService();
        HashtagService hashtagService = new HashtagService();
        DealScoreService dealScoreService = new DealScoreService(categoryService);
        TrendingInstagramAudioService trendingAudioService = new TrendingInstagramAudioService();
        VideoGenerationService videoGenService = new VideoGenerationService(trendingAudioService);

        youtubeService = new YouTubeService(categoryService, dealScoreService, null, videoGenService);
    }

    @Test
    void testGenerateShortsPackage() {
        Deal deal = new Deal("boAt Airdopes 141 Bluetooth Truly Wireless in Ear Earbuds", "999", "img", "https://amazon.in/dp/B09V36YJZW", "Amazon");
        deal.setMrp("4490");

        Map<String, Object> pack = youtubeService.generateShortsPackage(deal);

        assertNotNull(pack);
        assertTrue(pack.containsKey("title"));
        assertTrue(pack.containsKey("description"));
        assertTrue(pack.containsKey("pinnedComment"));
        assertTrue(pack.containsKey("tags"));
        assertTrue(pack.containsKey("videoStreamUrl"));

        String title = (String) pack.get("title");
        assertTrue(title.contains("#shorts"));
        assertTrue(title.contains("OFF") || title.contains("boAt"));

        String description = (String) pack.get("description");
        assertTrue(description.contains("Deal Price: ₹999"));
        assertTrue(description.contains("MRP: ₹4490"));
        assertTrue(description.contains("Affiliate Disclosure"));
        assertTrue(description.contains("https://t.me/"));

        String pinnedComment = (String) pack.get("pinnedComment");
        assertTrue(pinnedComment.contains("Direct Purchase Link"));
        assertTrue(pinnedComment.contains("https://t.me/"));
    }
}
