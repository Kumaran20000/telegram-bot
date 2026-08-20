package com.example.telegram_bot.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.telegram_bot.model.Deal;
import com.example.telegram_bot.model.ProductCategory;

class CarouselServiceTest {

    private CarouselService carouselService;

    @BeforeEach
    void setUp() {
        CategoryService categoryService = new CategoryService();
        HashtagService hashtagService = new HashtagService();
        MessageFormatterService messageFormatterService = new MessageFormatterService(categoryService, hashtagService);

        carouselService = new CarouselService(
                null, categoryService, hashtagService, null, null, messageFormatterService);
    }

    @Test
    void testBuildCarouselCaptionHasNoHtmlTags() {
        Deal d1 = new Deal("boAt Airdopes 141 Bluetooth Truly Wireless Earbuds", "999", "img1", "link1", "Amazon");
        d1.setMrp("4490");
        Deal d2 = new Deal("Sony WH-1000XM5 Wireless Noise Cancelling Headphones", "24990", "img2", "link2", "Amazon");
        d2.setMrp("34990");

        String caption = carouselService.buildCarouselCaption(ProductCategory.HEADPHONE, List.of(d1, d2));

        assertNotNull(caption);
        assertFalse(caption.contains("<b>") || caption.contains("</b>"), "Caption must not contain <b> tags");
        assertFalse(caption.contains("<a") || caption.contains("</a>"), "Caption must not contain <a> tags");
        assertFalse(caption.contains("<s>") || caption.contains("</s>"), "Caption must not contain <s> tags");

        assertTrue(caption.contains("HEADPHONE DEALS"));
        assertTrue(caption.contains("boAt Airdopes 141"));
        assertTrue(caption.contains("Deal Price: ₹999"));
        assertTrue(caption.contains("Comment \"LINK\""));
        assertTrue(caption.contains("#headphone") || caption.contains("#boat"));
    }
}
