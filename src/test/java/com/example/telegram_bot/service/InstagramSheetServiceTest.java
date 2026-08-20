package com.example.telegram_bot.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.telegram_bot.model.Deal;

class InstagramSheetServiceTest {

    private InstagramSheetService instagramSheetService;

    @BeforeEach
    void setUp() {
        CategoryService categoryService = new CategoryService();
        HashtagService hashtagService = new HashtagService();
        DealScoreService dealScoreService = new DealScoreService(categoryService);
        TrendingInstagramAudioService audioService = new TrendingInstagramAudioService();

        instagramSheetService = new InstagramSheetService(
                null, null, categoryService, hashtagService, null, dealScoreService, audioService);
    }

    @Test
    void testDetermineSmartGroupHeadsets() {
        Deal deal = new Deal("boAt Airdopes 141 Bluetooth Truly Wireless Earbuds", "999", "img", "link", "Amazon");
        String group = instagramSheetService.determineSmartGroup(deal);
        assertEquals("Headsets & Audio", group);
    }

    @Test
    void testDetermineSmartGroupSmartwatches() {
        Deal deal = new Deal("Noise ColorFit Pulse 2 Smart Watch with Bluetooth Calling", "1499", "img", "link", "Amazon");
        String group = instagramSheetService.determineSmartGroup(deal);
        assertEquals("Smartwatches", group);
    }

    @Test
    void testDetermineSmartGroupGadgets() {
        Deal deal = new Deal("Apple iPhone 15 Pro Max 256GB Titanium", "134999", "img", "link", "Amazon");
        String group = instagramSheetService.determineSmartGroup(deal);
        assertEquals("Top Smartphones", group);
    }

    @Test
    void testDetermineSmartGroupKitchen() {
        Deal deal = new Deal("Pigeon Healthifry Digital Air Fryer 4.2L", "2499", "img", "link", "Amazon");
        String group = instagramSheetService.determineSmartGroup(deal);
        assertEquals("Smart Kitchen Finds", group);
    }

    @Test
    void testBuildGroupCarouselCaption() {
        Deal d1 = new Deal("boAt Airdopes 141 Earbuds", "999", "img1", "link1", "Amazon");
        d1.setMrp("4490");
        Deal d2 = new Deal("Sony WH-1000XM5 Wireless Headphones", "24990", "img2", "link2", "Amazon");
        d2.setMrp("34990");

        String caption = instagramSheetService.buildGroupCarouselCaption("Headsets & Audio", List.of(d1, d2));
        assertNotNull(caption);
        assertTrue(caption.contains("HEADSETS & AUDIO"));
        assertTrue(caption.contains("boAt Airdopes 141"));
        assertTrue(caption.contains("Comment \"LINK\""));
        assertFalse(caption.contains("<b>") || caption.contains("</b>") || caption.contains("<a") || caption.contains("</a>"),
                "Instagram Carousel caption must not contain HTML tags like <b> or <a>");
        assertTrue(caption.contains("#earbuds") || caption.contains("#boat"));
    }
}
