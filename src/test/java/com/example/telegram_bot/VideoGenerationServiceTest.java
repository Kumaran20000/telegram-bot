package com.example.telegram_bot;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.example.telegram_bot.model.Deal;
import com.example.telegram_bot.service.VideoGenerationService;
import com.example.telegram_bot.service.VideoGenerationService.ReelTemplate;

public class VideoGenerationServiceTest {

    @Test
    public void testCreatePostImageWithPriceAndOfferPrice() throws Exception {
        VideoGenerationService service = new VideoGenerationService();

        Deal deal = new Deal();
        deal.setTitle("220 GSM Ultra-Soft Luxury AC Comforter Set Double Bed");
        deal.setPrice("1749");
        deal.setMrp("2999");
        deal.setDiscount("42%");
        deal.setImage("src/main/resources/images/product.jpg");

        String outputPath = "generated/post_image.jpg";
        String resultPath = service.createPostImage(deal, outputPath);

        assertNotNull(resultPath);
        File outputFile = new File(resultPath);
        assertTrue(outputFile.exists(), "The generated post image file should exist");
        assertTrue(outputFile.length() > 0, "The generated image file size should be greater than 0");
    }

    @Test
    public void testRenderAllSixScenesAsPreviewImages() throws Exception {
        VideoGenerationService service = new VideoGenerationService();

        Deal deal = new Deal();
        deal.setTitle("220 GSM Ultra-Soft Luxury AC Comforter Set Double Bed");
        deal.setPrice("1749");
        deal.setMrp("2999");
        deal.setDiscount("42%");
        deal.setImage("src/main/resources/images/product.jpg");

        File outDir = new File("generated");
        if (!outDir.exists()) outDir.mkdirs();

        for (int scene = 1; scene <= 6; scene++) {
            BufferedImage sceneImg = service.renderScenePreview(deal, scene);
            assertNotNull(sceneImg, "Scene " + scene + " image should not be null");
            File sceneFile = new File("generated/scene_" + scene + ".jpg");
            ImageIO.write(sceneImg, "jpg", sceneFile);
            assertTrue(sceneFile.exists(), "Scene " + scene + " file should exist");
            assertTrue(sceneFile.length() > 0, "Scene " + scene + " file should have content");
        }
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

    @Test
    public void testAudioThemes() {
        assertNotNull(VideoGenerationService.AUDIO_THEMES);
        assertEquals(6, VideoGenerationService.AUDIO_THEMES.length);
        for (String theme : VideoGenerationService.AUDIO_THEMES) {
            assertTrue(theme.contains("BPM") && theme.contains("Trending"), 
                    "Each audio theme must be upbeat with BPM and trending indicator: " + theme);
        }
        assertTrue(VideoGenerationService.AUDIO_THEMES[0].contains("Electro Tech House"));
        assertTrue(VideoGenerationService.AUDIO_THEMES[1].contains("Pop & Slap Bass"));
        assertTrue(VideoGenerationService.AUDIO_THEMES[2].contains("Drum & Bass"));
        assertTrue(VideoGenerationService.AUDIO_THEMES[3].contains("Phonk & 808"));
        assertTrue(VideoGenerationService.AUDIO_THEMES[4].contains("Tech House Groove"));
        assertTrue(VideoGenerationService.AUDIO_THEMES[5].contains("Price-Crash"));
    }

    @Test
    public void testReelTemplateEnum() {
        assertEquals(7, ReelTemplate.values().length);
        assertEquals(ReelTemplate.TEMPLATE_A_PRODUCT_FOCUSED, ReelTemplate.values()[0]);
        assertEquals(ReelTemplate.TEMPLATE_B_PRICE_DROP, ReelTemplate.values()[1]);
        assertEquals(ReelTemplate.TEMPLATE_C_BIG_DISCOUNT, ReelTemplate.values()[2]);
        assertEquals(ReelTemplate.TEMPLATE_D_UNDER_999, ReelTemplate.values()[3]);
        assertEquals(ReelTemplate.TEMPLATE_E_TOP_RATED, ReelTemplate.values()[4]);
        assertEquals(ReelTemplate.TEMPLATE_F_FLASH_DEAL, ReelTemplate.values()[5]);
        assertEquals(ReelTemplate.TEMPLATE_G_COMPARISON, ReelTemplate.values()[6]);
    }

    @Test
    public void testExtractProductBenefits() {
        VideoGenerationService service = new VideoGenerationService();

        // 1. Test Comforter Set with GSM
        Deal comforterDeal = new Deal();
        comforterDeal.setTitle("220 GSM Ultra-Soft Luxury AC Comforter Set Double Bed");
        List<String> comforterBenefits = service.extractProductBenefits(comforterDeal);
        assertNotNull(comforterBenefits);
        assertFalse(comforterBenefits.isEmpty());
        assertTrue(comforterBenefits.stream().anyMatch(b -> b.contains("220 GSM") || b.contains("Soft") || b.contains("Bed")));

        // 2. Test Wireless Earbuds
        Deal earbudsDeal = new Deal();
        earbudsDeal.setTitle("boAt Airdopes 141 ANC TWS Earbuds 42H Playtime");
        List<String> earbudBenefits = service.extractProductBenefits(earbudsDeal);
        assertNotNull(earbudBenefits);
        assertTrue(earbudBenefits.stream().anyMatch(b -> b.contains("Noise Cancellation") || b.contains("Audio") || b.contains("Battery")));
    }

    @Test
    public void testTemplateDeterminationAndHooks() {
        VideoGenerationService service = new VideoGenerationService();

        // High discount deal (60% off) -> Template C
        Deal highDiscountDeal = new Deal();
        highDiscountDeal.setTitle("220 GSM Luxury Comforter");
        highDiscountDeal.setPrice("1749");
        highDiscountDeal.setMrp("4399");
        highDiscountDeal.setDiscount("60%");

        ReelTemplate tpl = service.determineTemplate(highDiscountDeal, -1);
        assertEquals(ReelTemplate.TEMPLATE_C_BIG_DISCOUNT, tpl);

        String hook = service.generateHookForTemplate(highDiscountDeal, tpl);
        assertTrue(hook.contains("60% OFF"));

        // Budget deal Under ₹999 (without huge discount) -> Template D
        Deal budgetDeal = new Deal();
        budgetDeal.setTitle("Casual Cotton T-Shirt");
        budgetDeal.setPrice("499");
        budgetDeal.setMrp("599");
        budgetDeal.setDiscount("16%");

        ReelTemplate budgetTpl = service.determineTemplate(budgetDeal, -1);
        assertEquals(ReelTemplate.TEMPLATE_D_UNDER_999, budgetTpl);

        String budgetHook = service.generateHookForTemplate(budgetDeal, budgetTpl);
        assertTrue(budgetHook.contains("UNDER ₹500") || budgetHook.contains("UNDER ₹999"));
    }

    @Test
    public void testExtractShortProductName() {
        VideoGenerationService service = new VideoGenerationService();
        assertEquals("Comforter Set", service.extractShortProductName("220 GSM Ultra-Soft Luxury AC Comforter Set Double Bed"));
        assertEquals("Earbuds", service.extractShortProductName("boAt Airdopes 141 ANC TWS Earbuds"));
        assertEquals("Smartwatch", service.extractShortProductName("Noise ColorFit Ultra Smartwatch"));
    }
}
