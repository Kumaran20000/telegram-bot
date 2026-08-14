package com.example.telegram_bot;

import com.example.telegram_bot.model.Deal;
import com.example.telegram_bot.service.VideoGenerationService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

public class VideoGenerationServiceTest {

    @Test
    public void testCreatePostImageWithPriceAndOfferPrice() throws Exception {
        VideoGenerationService service = new VideoGenerationService();

        Deal deal = new Deal();
        deal.setTitle("pTron Bassbuds Duo Wireless Earbuds");
        deal.setPrice("799");
        deal.setMrp("2599");
        deal.setDiscount("69%");
        deal.setImage("https://dummyimage.com/600x600/ffffff/000000.jpg&text=Earbuds");

        String outputPath = "generated/test_post_image.jpg";
        String resultPath = service.createPostImage(deal, outputPath);

        assertNotNull(resultPath);
        File outputFile = new File(resultPath);
        assertTrue(outputFile.exists(), "The generated post image file should exist");
        assertTrue(outputFile.length() > 0, "The generated image file size should be greater than 0");
    }

    @Test
    public void testBrandBadgeDetection() {
        VideoGenerationService service = new VideoGenerationService();

        VideoGenerationService.BrandBadge apple = service.detectBrandBadge("Apple iPhone 15 Pro Max (256GB)");
        assertNotNull(apple);
        assertEquals("🍎 APPLE", apple.getName());

        VideoGenerationService.BrandBadge boat = service.detectBrandBadge("boAt Airdopes 141 TWS Earbuds");
        assertNotNull(boat);
        assertEquals("⚡ boAt", boat.getName());

        VideoGenerationService.BrandBadge samsung = service.detectBrandBadge("Samsung Galaxy S24 Ultra 5G");
        assertNotNull(samsung);
        assertEquals("📱 SAMSUNG", samsung.getName());

        VideoGenerationService.BrandBadge sony = service.detectBrandBadge("Sony WH-1000XM5 Wireless Headphones");
        assertNotNull(sony);
        assertEquals("🎧 SONY", sony.getName());
    }
}
