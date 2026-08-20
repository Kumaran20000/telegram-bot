package com.example.telegram_bot.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Service;

import com.example.telegram_bot.model.Deal;
import com.example.telegram_bot.model.ProductCategory;
import com.example.telegram_bot.model.TrendingAudioTrack;

@Service
public class VideoGenerationService {

    public enum ReelTemplate {
        TEMPLATE_A_PRODUCT_FOCUSED("Template A (Product-Focused)", "Hero product aesthetic with deep visual showcase"),
        TEMPLATE_B_PRICE_DROP("Template B (Price-Drop)", "Dramatic MRP to Deal price crash"),
        TEMPLATE_C_BIG_DISCOUNT("Template C (Big-Discount)", "High % verified discount focus"),
        TEMPLATE_D_UNDER_999("Template D (Under ₹999)", "Budget impulse-buy steal deal"),
        TEMPLATE_E_TOP_RATED("Template E (Top-Rated)", "4.5+ Star Amazon Bestseller pick"),
        TEMPLATE_F_FLASH_DEAL("Template F (Flash Deal)", "Limited-time flash sale alert"),
        TEMPLATE_G_COMPARISON("Template G (Value Comparison)", "High value comparison deal");

        private final String displayName;
        private final String description;

        ReelTemplate(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    private final TrendingInstagramAudioService trendingAudioService;
    private final AtomicInteger audioThemePointer = new AtomicInteger(0);

    public VideoGenerationService() {
        this(new TrendingInstagramAudioService());
    }

    public VideoGenerationService(TrendingInstagramAudioService trendingAudioService) {
        this.trendingAudioService = trendingAudioService != null ? trendingAudioService : new TrendingInstagramAudioService();
    }

    public TrendingInstagramAudioService getTrendingAudioService() {
        return trendingAudioService;
    }

    /**
     * Upbeat, high-energy, currently trending synthesized audio themes (120-135 BPM)
     * paired with Instagram's Reels Audio Library trending sounds (↗️ indicator).
     */
    public static final String[] AUDIO_THEMES = {
            "⚡ High-Energy Electro Tech House (128 BPM) [↗️ Trending]",
            "🔥 Viral Upbeat Pop & Slap Bass (126 BPM) [↗️ Trending]",
            "🚀 Dynamic Drum & Bass Fast Cadence (132 BPM) [↗️ Trending]",
            "💎 Modern Phonk & 808 Tech Drop (130 BPM) [↗️ Trending]",
            "✨ Upbeat Commercial Tech House Groove (126 BPM) [↗️ Trending]",
            "💥 Explosive Price-Crash Steal Beat (128 BPM) [↗️ Trending]"
    };

    // =========================================================================
    // REEL CREATION ENTRYPOINTS
    // =========================================================================

    public String createReel(Deal deal) throws Exception {
        return createReel(deal, -1);
    }

    public String createReel(Deal deal, int formatIndex) throws Exception {
        ReelTemplate template = determineTemplate(deal, formatIndex);
        String hookText = generateHookForTemplate(deal, template);
        List<String> benefits = extractProductBenefits(deal);

        return createMultiSceneReel(
                deal.getImage(),
                deal.getTitle(),
                deal.getPrice(),
                deal.getMrp(),
                deal.calculateDiscountPercent(),
                deal.calculateSavingsAmount(),
                template,
                hookText,
                benefits
        );
    }

    public String createReel(Deal deal, ReelTemplate template) throws Exception {
        String hookText = generateHookForTemplate(deal, template);
        List<String> benefits = extractProductBenefits(deal);

        return createMultiSceneReel(
                deal.getImage(),
                deal.getTitle(),
                deal.getPrice(),
                deal.getMrp(),
                deal.calculateDiscountPercent(),
                deal.calculateSavingsAmount(),
                template,
                hookText,
                benefits
        );
    }

    public String createReel(String imageUrl) throws Exception {
        return createReel(imageUrl, "Curated Special Deal", "", null, 0, 0, null);
    }

    public String createReel(String imageUrl, String titleText, String priceText) throws Exception {
        return createReel(imageUrl, titleText, priceText, null, 0, 0, null);
    }

    public String createReel(String imageUrl, String titleText, String priceText, String mrpText, int discountPercent, long savingsAmount) throws Exception {
        return createReel(imageUrl, titleText, priceText, mrpText, discountPercent, savingsAmount, null);
    }

    public String createReel(String imageUrl, String titleText, String priceText, String mrpText, int discountPercent, long savingsAmount, String hookBannerText) throws Exception {
        Deal mockDeal = new Deal();
        mockDeal.setImage(imageUrl);
        mockDeal.setTitle(titleText != null ? titleText : "Special Offer");
        mockDeal.setPrice(priceText);
        mockDeal.setMrp(mrpText);
        if (discountPercent > 0) mockDeal.setDiscount(discountPercent + "%");

        ReelTemplate template = determineTemplate(mockDeal, -1);
        String finalHook = (hookBannerText != null && !hookBannerText.isEmpty()) ? hookBannerText : generateHookForTemplate(mockDeal, template);
        List<String> benefits = extractProductBenefits(mockDeal);

        return createMultiSceneReel(
                imageUrl,
                titleText,
                priceText,
                mrpText,
                discountPercent,
                savingsAmount,
                template,
                finalHook,
                benefits
        );
    }

    /**
     * Creates a high-retention 6-scene 1080x1920 Instagram Reel video (17 seconds @ 30 FPS = 510 frames).
     */
    public String createMultiSceneReel(
            String imageUrl,
            String titleText,
            String priceText,
            String mrpText,
            int discountPercent,
            long savingsAmount,
            ReelTemplate template,
            String hookText,
            List<String> benefits) throws Exception {

        File outputDir = new File("generated");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        String output = "generated/reel.mp4";
        BufferedImage productImage = loadImage(imageUrl);

        int themeIdx = audioThemePointer.getAndIncrement() % AUDIO_THEMES.length;
        String themeName = AUDIO_THEMES[themeIdx];
        TrendingAudioTrack pairedAudio = trendingAudioService.getAllTracks().get(themeIdx % trendingAudioService.getAllTracks().size());
        System.out.println("🎬 Generating 6-Scene Instagram Reel (" + template.getDisplayName() + ")");
        System.out.println("   ⚡ Upbeat Audio Theme: " + themeName);
        System.out.println("   🎵 Paired Instagram Reels Library Track: " + pairedAudio.getTitle() + " - " + pairedAudio.getArtist() + " (↗️ " + pairedAudio.getBpm() + " BPM)");
        System.out.println("   🔍 Instagram Reels Audio Search: \"" + pairedAudio.getInstagramSearchQuery() + "\"");

        Java2DFrameConverter converter = new Java2DFrameConverter();
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(output, 1080, 1920, 1);
        recorder.setFormat("mp4");
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setFrameRate(30);
        recorder.setVideoBitrate(6_000_000); // 6 Mbps for crisp 1080p HD
        recorder.setVideoOption("preset", "medium");
        recorder.setVideoOption("crf", "18");
        recorder.setOption("movflags", "+faststart");

        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        recorder.setAudioBitrate(128000);
        recorder.setSampleRate(44100);

        recorder.start();

        int totalFrames = 510;
        for (int i = 0; i < totalFrames; i++) {
            BufferedImage canvas = renderStoryboardFrame(
                    productImage,
                    titleText,
                    priceText,
                    mrpText,
                    discountPercent,
                    savingsAmount,
                    template,
                    hookText,
                    benefits,
                    i,
                    totalFrames
            );

            Frame frame = converter.convert(canvas);
            recorder.record(frame);

            short[] audioBuffer = generateMultiTrackAudioFrame(themeIdx, i, totalFrames, 1470);
            recorder.recordSamples(java.nio.ShortBuffer.wrap(audioBuffer));
        }

        recorder.stop();
        recorder.release();

        System.out.println("✅ High-converting 6-Scene Reel created successfully: " + output);
        return output;
    }

    /**
     * Renders a specific scene (1-6) as a static BufferedImage for instant visual preview/debugging.
     */
    public BufferedImage renderScenePreview(Deal deal, int sceneNumber) throws Exception {
        BufferedImage productImage = loadImage(deal != null ? deal.getImage() : null);
        ReelTemplate template = determineTemplate(deal, -1);
        String hookText = generateHookForTemplate(deal, template);
        List<String> benefits = extractProductBenefits(deal);

        int targetFrame = switch (sceneNumber) {
            case 1 -> 30;
            case 2 -> 120;
            case 3 -> 240;
            case 4 -> 360;
            case 5 -> 450;
            case 6 -> 505;
            default -> 120;
        };

        return renderStoryboardFrame(
                productImage,
                deal != null ? deal.getTitle() : "Special Offer",
                deal != null ? deal.getPrice() : "999",
                deal != null ? deal.getMrp() : null,
                deal != null ? deal.calculateDiscountPercent() : 0,
                deal != null ? deal.calculateSavingsAmount() : 0,
                template,
                hookText,
                benefits,
                targetFrame,
                510
        );
    }

    // =========================================================================
    // MULTI-SCENE STORYBOARD FRAME RENDERER
    // =========================================================================

    /**
     * Renders a single frame according to the active scene in the storyboard:
     * - Scene 1 (0 to 2.0s / frames 0-60): THE HOOK (Scroll-stopping text + price alert)
     * - Scene 2 (2.0 to 6.0s / frames 60-180): THE PRODUCT HERO (Big clear showcase 75%+ screen)
     * - Scene 3 (6.0 to 10.0s / frames 180-300): PRICE DROP (MRP strikethrough vs Offer Price)
     * - Scene 4 (10.0 to 14.0s / frames 300-420): 3 KEY BENEFITS (Value highlights with green checks)
     * - Scene 5 (14.0 to 16.5s / frames 420-495): DIRECT SINGLE CTA (Comment "LINK" in DM)
     * - Scene 6 (16.5 to 17.0s / frames 495-510): BRAND OUTRO (OfferZone Daily Deals)
     */
    public BufferedImage renderStoryboardFrame(
            BufferedImage productImage,
            String titleText,
            String priceText,
            String mrpText,
            int discountPercent,
            long savingsAmount,
            ReelTemplate template,
            String hookText,
            List<String> benefits,
            int frameIndex,
            int totalFrames) {

        BufferedImage canvas = new BufferedImage(1080, 1920, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = canvas.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. Sleek Modern Normal Clean Background Gradient (#F8FAFC -> #E2E8F0)
        GradientPaint bgGradient = new GradientPaint(0, 0, new Color(248, 250, 252), 0, 1920, new Color(226, 232, 240));
        g.setPaint(bgGradient);
        g.fillRect(0, 0, 1080, 1920);

        // Ambient radial subtle studio highlight
        Point2D center = new Point2D.Float(540, 750);
        float radius = 600;
        float[] dist = {0.0f, 1.0f};
        Color[] colors = {new Color(255, 255, 255, 220), new Color(241, 245, 249, 0)};
        RadialGradientPaint ambientGlow = new RadialGradientPaint(center, radius, dist, colors);
        g.setPaint(ambientGlow);
        g.fillOval(40, 200, 1000, 1100);

        // Top Subtle Watermark Header Pill (Safe zone: Y=70)
        int wmPillW = 680;
        int wmPillH = 50;
        int wmPillX = (1080 - wmPillW) / 2;
        int wmPillY = 70;
        g.setColor(new Color(255, 255, 255, 230));
        g.fillRoundRect(wmPillX, wmPillY, wmPillW, wmPillH, 25, 25);
        g.setColor(new Color(203, 213, 225));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(wmPillX, wmPillY, wmPillW, wmPillH, 25, 25);

        g.setColor(new Color(71, 85, 105)); // Slate 600
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        FontMetrics fmW = g.getFontMetrics();
        String watermark = "OFFERZONE  •  VERIFIED AMAZON DEALS";
        int wX = (1080 - fmW.stringWidth(watermark)) / 2;
        g.drawString(watermark, wX, wmPillY + 33);

        // Format clean price values
        String formattedPrice = "";
        if (priceText != null && !priceText.isEmpty() && !priceText.equalsIgnoreCase("N/A")) {
            formattedPrice = priceText.startsWith("₹") ? priceText : "₹" + priceText;
        }
        String formattedMrp = "";
        if (mrpText != null && !mrpText.isEmpty() && !mrpText.equalsIgnoreCase("N/A")) {
            formattedMrp = mrpText.startsWith("₹") ? mrpText : "₹" + mrpText;
        } else if (discountPercent > 0 && !formattedPrice.isEmpty()) {
            try {
                double p = Double.parseDouble(formattedPrice.replaceAll("[^0-9.]", ""));
                long calcMrp = Math.round(p / (1.0 - (discountPercent / 100.0)));
                if (calcMrp > p) formattedMrp = "₹" + String.format("%,d", calcMrp);
            } catch (Exception ignored) {}
        }

        // =====================================================================
        // SCENE LOGIC BY FRAME INDEX
        // =====================================================================

        if (frameIndex < 60) {
            // -----------------------------------------------------------------
            // SCENE 1 (0-2s): THE HOOK
            // -----------------------------------------------------------------
            double sceneProgress = (double) frameIndex / 60.0;
            double zoom = 1.0 + (sceneProgress * 0.05);

            // Large product image card in background
            if (productImage != null) {
                int bgImgSize = (int) (700 * zoom);
                int bgImgX = (1080 - bgImgSize) / 2;
                int bgImgY = 300;

                // Card shadow & clean white background
                g.setColor(new Color(0, 0, 0, 20));
                g.fillRoundRect(bgImgX - 6, bgImgY + 10, bgImgSize + 12, bgImgSize + 12, 44, 44);

                g.setColor(Color.WHITE);
                g.fillRoundRect(bgImgX, bgImgY, bgImgSize, bgImgSize, 40, 40);

                Shape oldClip = g.getClip();
                g.setClip(new RoundRectangle2D.Double(bgImgX + 16, bgImgY + 16, bgImgSize - 32, bgImgSize - 32, 32, 32));
                g.drawImage(productImage.getScaledInstance(bgImgSize - 32, bgImgSize - 32, Image.SCALE_SMOOTH), bgImgX + 16, bgImgY + 16, null);
                g.setClip(oldClip);
            }

            // Big Centered Hook Banner Card
            int cardW = 940;
            int cardH = 340;
            int cardX = (1080 - cardW) / 2;
            int cardY = 1080;

            // Card Shadow
            g.setColor(new Color(0, 0, 0, 25));
            g.fillRoundRect(cardX - 4, cardY + 8, cardW + 8, cardH + 8, 38, 38);

            // Card White Background
            g.setColor(Color.WHITE);
            g.fillRoundRect(cardX, cardY, cardW, cardH, 36, 36);

            // Amber Accent Border
            g.setColor(new Color(245, 158, 11)); // Warm Amber
            g.setStroke(new BasicStroke(3.0f));
            g.drawRoundRect(cardX, cardY, cardW, cardH, 36, 36);

            // Hook Top Tag Pill
            int tagW = 480;
            int tagH = 50;
            int tagX = (1080 - tagW) / 2;
            int tagY = cardY + 28;
            g.setColor(new Color(254, 243, 199)); // Light Amber pill
            g.fillRoundRect(tagX, tagY, tagW, tagH, 25, 25);

            g.setFont(new Font("SansSerif", Font.BOLD, 24));
            g.setColor(new Color(180, 83, 9)); // Amber 700
            FontMetrics fmTag = g.getFontMetrics();
            String tagText = "⚡ LIMITED TIME DEAL ALERT ⚡";
            g.drawString(tagText, (1080 - fmTag.stringWidth(tagText)) / 2, tagY + 34);

            // Big Hook Text
            g.setFont(new Font("SansSerif", Font.BOLD, 46));
            g.setColor(new Color(15, 23, 42)); // Dark Slate
            String mainHook = (hookText != null && !hookText.isEmpty()) ? hookText : "🔥 HUGE DEAL ALERT!";
            drawWrappedString(g, mainHook, cardX + 30, cardY + 140, cardW - 60, 54, 2);

            // Price sub-badge in hook if available
            if (!formattedPrice.isEmpty()) {
                int pillW = 540;
                int pillH = 60;
                int pillX = (1080 - pillW) / 2;
                int pillY = cardY + 250;

                GradientPaint pGrad = new GradientPaint(pillX, pillY, new Color(5, 150, 105), pillX + pillW, pillY + pillH, new Color(16, 185, 129));
                g.setPaint(pGrad);
                g.fillRoundRect(pillX, pillY, pillW, pillH, 30, 30);

                g.setFont(new Font("SansSerif", Font.BOLD, 32));
                g.setColor(Color.WHITE);
                String priceHook = "GRAB FOR ONLY " + formattedPrice;
                FontMetrics fmPH = g.getFontMetrics();
                g.drawString(priceHook, (1080 - fmPH.stringWidth(priceHook)) / 2, pillY + 42);
            }

        } else if (frameIndex < 180) {
            // -----------------------------------------------------------------
            // SCENE 2 (2-6s): THE PRODUCT AS HERO
            // -----------------------------------------------------------------
            double sceneProgress = (double) (frameIndex - 60) / 120.0;
            double zoom = 1.0 + (sceneProgress * 0.04);

            // Large Hero Product Card (Occupies 75%+ of canvas width)
            int cardW = 920;
            int cardH = 920;
            int cardX = (1080 - cardW) / 2;
            int cardY = 220;

            // Card Shadow
            g.setColor(new Color(0, 0, 0, 25));
            g.fillRoundRect(cardX - 4, cardY + 10, cardW + 8, cardH + 10, 44, 44);

            // Card Pure White Fill & Border
            g.setColor(Color.WHITE);
            g.fillRoundRect(cardX, cardY, cardW, cardH, 40, 40);
            g.setColor(new Color(226, 232, 240));
            g.setStroke(new BasicStroke(2.0f));
            g.drawRoundRect(cardX, cardY, cardW, cardH, 40, 40);

            // Product Image with subtle Ken Burns zoom
            if (productImage != null) {
                double baseScale = Math.min(
                        (double) (cardW - 80) / productImage.getWidth(),
                        (double) (cardH - 80) / productImage.getHeight()
                );
                double finalScale = baseScale * zoom;
                int imgW = (int) (productImage.getWidth() * finalScale);
                int imgH = (int) (productImage.getHeight() * finalScale);

                Shape oldClip = g.getClip();
                g.setClip(new RoundRectangle2D.Double(cardX + 20, cardY + 20, cardW - 40, cardH - 40, 32, 32));
                int imgX = cardX + (cardW - imgW) / 2;
                int imgY = cardY + (cardH - imgH) / 2;
                g.drawImage(productImage.getScaledInstance(imgW, imgH, Image.SCALE_SMOOTH), imgX, imgY, null);
                g.setClip(oldClip);
            }

            // Top Badges on Hero Card
            BrandBadge brand = detectBrandBadge(titleText);
            if (brand != null) {
                g.setFont(new Font("SansSerif", Font.BOLD, 26));
                FontMetrics fmBrand = g.getFontMetrics();
                int badgeW = fmBrand.stringWidth(brand.getName()) + 32;
                int badgeH = 54;
                int badgeX = (cardX + cardW) - badgeW - 24;
                int badgeY = cardY + 24;

                g.setColor(new Color(15, 23, 42));
                g.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 18, 18);
                g.setColor(Color.WHITE);
                g.drawString(brand.getName(), badgeX + 16, badgeY + 37);
            }

            // Left Pill: Discount or Top Rated
            int leftBadgeX = cardX + 24;
            int leftBadgeY = cardY + 24;
            int leftBadgeW = 220;
            int leftBadgeH = 54;
            g.setColor(new Color(5, 150, 105)); // Emerald
            g.fillRoundRect(leftBadgeX, leftBadgeY, leftBadgeW, leftBadgeH, 18, 18);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 26));
            String lText = discountPercent > 0 ? "🏷️ " + discountPercent + "% OFF" : "⭐ TOP RATED";
            FontMetrics fmL = g.getFontMetrics();
            g.drawString(lText, leftBadgeX + (leftBadgeW - fmL.stringWidth(lText)) / 2, leftBadgeY + 37);

            // Clean Product Title Below Hero Card (Max 2 lines, large & legible)
            if (titleText != null && !titleText.isEmpty()) {
                g.setColor(new Color(15, 23, 42)); // Dark Slate
                g.setFont(new Font("SansSerif", Font.BOLD, 42));
                drawWrappedString(g, titleText, 80, 1210, 920, 54, 2);
            }

            // Micro Feature Badges Row
            int pillsY = 1350;
            renderMicroPill(g, 100, pillsY, "⭐ Verified Deal", new Color(255, 255, 255));
            renderMicroPill(g, 380, pillsY, "✨ Premium Quality", new Color(255, 255, 255));
            renderMicroPill(g, 700, pillsY, "🚚 Fast Shipping", new Color(255, 255, 255));

        } else if (frameIndex < 300) {
            // -----------------------------------------------------------------
            // SCENE 3 (6-10s): THE PRICE DROP & SAVINGS
            // -----------------------------------------------------------------
            int cardSize = 580;
            int cardX = (1080 - cardSize) / 2;
            int cardY = 180;

            // Card Shadow & White Fill
            g.setColor(new Color(0, 0, 0, 20));
            g.fillRoundRect(cardX - 4, cardY + 8, cardSize + 8, cardSize + 8, 36, 36);

            g.setColor(Color.WHITE);
            g.fillRoundRect(cardX, cardY, cardSize, cardSize, 32, 32);
            g.setColor(new Color(226, 232, 240));
            g.setStroke(new BasicStroke(2.0f));
            g.drawRoundRect(cardX, cardY, cardSize, cardSize, 32, 32);

            if (productImage != null) {
                double scale = Math.min(
                        (double) (cardSize - 40) / productImage.getWidth(),
                        (double) (cardSize - 40) / productImage.getHeight()
                );
                int imgW = (int) (productImage.getWidth() * scale);
                int imgH = (int) (productImage.getHeight() * scale);
                Shape oldClip = g.getClip();
                g.setClip(new RoundRectangle2D.Double(cardX + 15, cardY + 15, cardSize - 30, cardSize - 30, 24, 24));
                g.drawImage(productImage.getScaledInstance(imgW, imgH, Image.SCALE_SMOOTH),
                        cardX + (cardSize - imgW) / 2, cardY + (cardSize - imgH) / 2, null);
                g.setClip(oldClip);
            }

            // High-Converting White Price Card in Bottom Half
            int priceBoxX = 80;
            int priceBoxY = 820;
            int priceBoxW = 920;
            int priceBoxH = 480;

            g.setColor(new Color(0, 0, 0, 25));
            g.fillRoundRect(priceBoxX - 4, priceBoxY + 8, priceBoxW + 8, priceBoxH + 8, 40, 40);

            g.setColor(Color.WHITE);
            g.fillRoundRect(priceBoxX, priceBoxY, priceBoxW, priceBoxH, 36, 36);

            g.setColor(new Color(16, 185, 129)); // Emerald Border
            g.setStroke(new BasicStroke(3.0f));
            g.drawRoundRect(priceBoxX, priceBoxY, priceBoxW, priceBoxH, 36, 36);

            // Card Header Pill
            int pHdrW = 460;
            int pHdrH = 50;
            int pHdrX = (1080 - pHdrW) / 2;
            int pHdrY = priceBoxY + 30;
            g.setColor(new Color(254, 243, 199)); // Light Amber
            g.fillRoundRect(pHdrX, pHdrY, pHdrW, pHdrH, 25, 25);

            g.setFont(new Font("SansSerif", Font.BOLD, 26));
            g.setColor(new Color(180, 83, 9)); // Gold/Amber
            FontMetrics fmHdr = g.getFontMetrics();
            String hdrText = "⚡ SPECIAL PRICE DROP ⚡";
            g.drawString(hdrText, (1080 - fmHdr.stringWidth(hdrText)) / 2, pHdrY + 35);

            // MRP Row (Strikethrough)
            if (!formattedMrp.isEmpty()) {
                g.setFont(new Font("SansSerif", Font.BOLD, 42));
                g.setColor(new Color(100, 116, 139)); // Muted Slate
                String mrpFull = "MRP: " + formattedMrp;
                FontMetrics fmMrp = g.getFontMetrics();
                int mrpX = (1080 - fmMrp.stringWidth(mrpFull)) / 2;
                int mrpY = priceBoxY + 155;
                g.drawString(mrpFull, mrpX, mrpY);

                // Strikethrough Line in Rose-Red
                int valX = mrpX + fmMrp.stringWidth("MRP: ");
                int valW = fmMrp.stringWidth(formattedMrp);
                g.setColor(new Color(225, 29, 72));
                g.setStroke(new BasicStroke(4.0f));
                g.drawLine(valX - 4, mrpY - 14, valX + valW + 4, mrpY - 14);
            }

            // Big Deal Price in Vivid Emerald Green
            if (!formattedPrice.isEmpty()) {
                g.setFont(new Font("SansSerif", Font.BOLD, 74));
                g.setColor(new Color(5, 150, 105)); // Emerald Green
                String dealStr = "DEAL: " + formattedPrice;
                FontMetrics fmDeal = g.getFontMetrics();
                int dealX = (1080 - fmDeal.stringWidth(dealStr)) / 2;
                g.drawString(dealStr, dealX, priceBoxY + 265);
            }

            // Discount Badge Pill
            if (discountPercent > 0 || savingsAmount > 0) {
                int pillW = 480;
                int pillH = 76;
                int pillX = (1080 - pillW) / 2;
                int pillY = priceBoxY + 345;

                GradientPaint pillGrad = new GradientPaint(pillX, pillY, new Color(5, 150, 105), pillX + pillW, pillY + pillH, new Color(16, 185, 129));
                g.setPaint(pillGrad);
                g.fillRoundRect(pillX, pillY, pillW, pillH, 28, 28);

                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, 36));
                String discText = discountPercent > 0 ? "🔥 FLAT " + discountPercent + "% OFF" : "⚡ SAVE ₹" + savingsAmount;
                FontMetrics fmDisc = g.getFontMetrics();
                g.drawString(discText, (1080 - fmDisc.stringWidth(discText)) / 2, pillY + 52);
            }

        } else if (frameIndex < 420) {
            // -----------------------------------------------------------------
            // SCENE 4 (10-14s): 3 KEY VALUE BENEFITS
            // -----------------------------------------------------------------
            int thumbSize = 360;
            int thumbX = (1080 - thumbSize) / 2;
            int thumbY = 160;

            g.setColor(new Color(0, 0, 0, 20));
            g.fillRoundRect(thumbX - 4, thumbY + 8, thumbSize + 8, thumbSize + 8, 32, 32);

            g.setColor(Color.WHITE);
            g.fillRoundRect(thumbX, thumbY, thumbSize, thumbSize, 28, 28);
            g.setColor(new Color(226, 232, 240));
            g.setStroke(new BasicStroke(2.0f));
            g.drawRoundRect(thumbX, thumbY, thumbSize, thumbSize, 28, 28);

            if (productImage != null) {
                double scale = Math.min(
                        (double) (thumbSize - 30) / productImage.getWidth(),
                        (double) (thumbSize - 30) / productImage.getHeight()
                );
                int imgW = (int) (productImage.getWidth() * scale);
                int imgH = (int) (productImage.getHeight() * scale);
                Shape oldClip = g.getClip();
                g.setClip(new RoundRectangle2D.Double(thumbX + 10, thumbY + 10, thumbSize - 20, thumbSize - 20, 20, 20));
                g.drawImage(productImage.getScaledInstance(imgW, imgH, Image.SCALE_SMOOTH),
                        thumbX + (thumbSize - imgW) / 2, thumbY + (thumbSize - imgH) / 2, null);
                g.setClip(oldClip);
            }

            // Benefits Section Header
            g.setFont(new Font("SansSerif", Font.BOLD, 38));
            g.setColor(new Color(15, 23, 42)); // Dark Slate
            FontMetrics fmH = g.getFontMetrics();
            String bHeader = "✨ WHY THIS IS WORTH BUYING ✨";
            g.drawString(bHeader, (1080 - fmH.stringWidth(bHeader)) / 2, 590);

            // 3 Benefit Cards Stack (Pure White Cards with Subtle Shadow)
            int cardW = 920;
            int cardH = 110;
            int cardX = 80;
            int startY = 660;
            int spacing = 135;

            List<String> activeBenefits = (benefits != null && !benefits.isEmpty()) ? benefits : extractProductBenefits(null);
            for (int b = 0; b < Math.min(3, activeBenefits.size()); b++) {
                int bY = startY + (b * spacing);

                g.setColor(new Color(0, 0, 0, 15));
                g.fillRoundRect(cardX - 2, bY + 6, cardW + 4, cardH + 4, 30, 30);

                g.setColor(Color.WHITE);
                g.fillRoundRect(cardX, bY, cardW, cardH, 28, 28);

                g.setColor(new Color(226, 232, 240));
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(cardX, bY, cardW, cardH, 28, 28);

                // Green Checkmark Icon Box
                g.setColor(new Color(5, 150, 105));
                g.fillRoundRect(cardX + 24, bY + 22, 66, 66, 18, 18);
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, 36));
                g.drawString("✓", cardX + 46, bY + 68);

                // Benefit Text
                g.setColor(new Color(15, 23, 42)); // Dark Slate
                g.setFont(new Font("SansSerif", Font.BOLD, 34));
                g.drawString(activeBenefits.get(b), cardX + 115, bY + 68);
            }

            // Price Reminder Pill
            if (!formattedPrice.isEmpty()) {
                int rPillW = 560;
                int rPillH = 64;
                int rPillX = (1080 - rPillW) / 2;
                int rPillY = 1120;
                g.setColor(new Color(236, 253, 245)); // Light Emerald
                g.fillRoundRect(rPillX, rPillY, rPillW, rPillH, 32, 32);
                g.setColor(new Color(16, 185, 129));
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(rPillX, rPillY, rPillW, rPillH, 32, 32);

                g.setFont(new Font("SansSerif", Font.BOLD, 32));
                g.setColor(new Color(5, 150, 105));
                String pReminder = "🔥 Deal Price: " + formattedPrice + " Only";
                FontMetrics fmPR = g.getFontMetrics();
                g.drawString(pReminder, (1080 - fmPR.stringWidth(pReminder)) / 2, rPillY + 44);
            }

        } else if (frameIndex < 495) {
            // -----------------------------------------------------------------
            // SCENE 5 (14-16.5s): DIRECT SINGLE CALL TO ACTION (CTA)
            // -----------------------------------------------------------------
            int ctaBoxW = 940;
            int ctaBoxH = 560;
            int ctaBoxX = (1080 - ctaBoxW) / 2;
            int ctaBoxY = 480;

            g.setColor(new Color(0, 0, 0, 25));
            g.fillRoundRect(ctaBoxX - 4, ctaBoxY + 8, ctaBoxW + 8, ctaBoxH + 8, 44, 44);

            g.setColor(Color.WHITE);
            g.fillRoundRect(ctaBoxX, ctaBoxY, ctaBoxW, ctaBoxH, 40, 40);

            g.setColor(new Color(79, 70, 229)); // Indigo Border
            g.setStroke(new BasicStroke(3.0f));
            g.drawRoundRect(ctaBoxX, ctaBoxY, ctaBoxW, ctaBoxH, 40, 40);

            // CTA Top Prompt
            g.setFont(new Font("SansSerif", Font.BOLD, 42));
            g.setColor(new Color(180, 83, 9)); // Amber / Gold
            FontMetrics fmCtaPrompt = g.getFontMetrics();
            String ctaPrompt = "🔥 WANT THIS DEAL? 🔥";
            g.drawString(ctaPrompt, (1080 - fmCtaPrompt.stringWidth(ctaPrompt)) / 2, ctaBoxY + 90);

            // Giant Action Pill
            int btnW = 820;
            int btnH = 130;
            int btnX = (1080 - btnW) / 2;
            int btnY = ctaBoxY + 160;

            GradientPaint btnGrad = new GradientPaint(btnX, btnY, new Color(37, 99, 235), btnX + btnW, btnY + btnH, new Color(79, 70, 229));
            g.setPaint(btnGrad);
            g.fillRoundRect(btnX, btnY, btnW, btnH, 36, 36);

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 52));
            String mainBtnText = "💬 COMMENT \"LINK\"";
            FontMetrics fmBtn = g.getFontMetrics();
            g.drawString(mainBtnText, (1080 - fmBtn.stringWidth(mainBtnText)) / 2, btnY + 84);

            // Subtitle
            g.setFont(new Font("SansSerif", Font.BOLD, 34));
            g.setColor(new Color(15, 23, 42)); // Slate 900
            String sub1 = "Direct buying link will be sent to your DM!";
            FontMetrics fmSub1 = g.getFontMetrics();
            g.drawString(sub1, (1080 - fmSub1.stringWidth(sub1)) / 2, ctaBoxY + 360);

            // Bio reminder
            g.setFont(new Font("SansSerif", Font.BOLD, 30));
            g.setColor(new Color(100, 116, 139)); // Slate 500
            String sub2 = "👉 Or click the Link in Bio to purchase directly";
            FontMetrics fmSub2 = g.getFontMetrics();
            g.drawString(sub2, (1080 - fmSub2.stringWidth(sub2)) / 2, ctaBoxY + 440);

        } else {
            // -----------------------------------------------------------------
            // SCENE 6 (16.5-17s): BRAND OUTRO
            // -----------------------------------------------------------------
            int outroW = 900;
            int outroH = 440;
            int outroX = (1080 - outroW) / 2;
            int outroY = 550;

            g.setColor(new Color(0, 0, 0, 25));
            g.fillRoundRect(outroX - 4, outroY + 8, outroW + 8, outroH + 8, 40, 40);

            g.setColor(Color.WHITE);
            g.fillRoundRect(outroX, outroY, outroW, outroH, 36, 36);

            g.setColor(new Color(59, 130, 246));
            g.setStroke(new BasicStroke(2.5f));
            g.drawRoundRect(outroX, outroY, outroW, outroH, 36, 36);

            // Channel Name
            g.setFont(new Font("SansSerif", Font.BOLD, 64));
            g.setColor(new Color(15, 23, 42)); // Dark Slate
            FontMetrics fmB = g.getFontMetrics();
            String brandTitle = "OfferZone 🔥";
            g.drawString(brandTitle, (1080 - fmB.stringWidth(brandTitle)) / 2, outroY + 120);

            // Subtitle
            g.setFont(new Font("SansSerif", Font.BOLD, 36));
            g.setColor(new Color(71, 85, 105));
            FontMetrics fmSub = g.getFontMetrics();
            String outroSub = "Follow for Daily Handpicked Deals";
            g.drawString(outroSub, (1080 - fmSub.stringWidth(outroSub)) / 2, outroY + 210);

            // Handle Pill
            int hPillW = 560;
            int hPillH = 76;
            int hPillX = (1080 - hPillW) / 2;
            int hPillY = outroY + 280;

            GradientPaint hGrad = new GradientPaint(hPillX, hPillY, new Color(5, 150, 105), hPillX + hPillW, hPillY + hPillH, new Color(16, 185, 129));
            g.setPaint(hGrad);
            g.fillRoundRect(hPillX, hPillY, hPillW, hPillH, 26, 26);

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 34));
            String handle = "❤️ @offerzone2538";
            FontMetrics fmHdl = g.getFontMetrics();
            g.drawString(handle, (1080 - fmHdl.stringWidth(handle)) / 2, hPillY + 50);
        }

        g.dispose();
        return canvas;
    }

    private void renderMicroPill(Graphics2D g, int x, int y, String text, Color bg) {
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text) + 32;
        int h = 50;

        g.setColor(new Color(0, 0, 0, 15));
        g.fillRoundRect(x - 2, y + 4, w + 4, h + 4, 18, 18);

        g.setColor(bg);
        g.fillRoundRect(x, y, w, h, 16, 16);

        g.setColor(new Color(203, 213, 225));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(x, y, w, h, 16, 16);

        g.setColor(new Color(51, 65, 85)); // Slate 700
        g.drawString(text, x + 16, y + 35);
    }

    // =========================================================================
    // TEMPLATE & HOOK RESOLUTION ENGINE
    // =========================================================================

    public ReelTemplate determineTemplate(Deal deal, int formatIndex) {
        if (formatIndex >= 0 && formatIndex < ReelTemplate.values().length) {
            return ReelTemplate.values()[formatIndex];
        }
        if (deal == null) return ReelTemplate.TEMPLATE_A_PRODUCT_FOCUSED;

        int discount = deal.calculateDiscountPercent();
        double priceVal = 0;
        try {
            if (deal.getPrice() != null) {
                priceVal = Double.parseDouble(deal.getPrice().replaceAll("[^0-9.]", ""));
            }
        } catch (Exception ignored) {}

        long savings = deal.calculateSavingsAmount();

        // 1. High discount >= 45% -> Template C
        if (discount >= 45) {
            return ReelTemplate.TEMPLATE_C_BIG_DISCOUNT;
        }
        // 2. Budget price <= 999 -> Template D
        if (priceVal > 0 && priceVal <= 999) {
            return ReelTemplate.TEMPLATE_D_UNDER_999;
        }
        // 3. Large savings >= ₹1,000 -> Template B (Price-Drop)
        if (savings >= 1000) {
            return ReelTemplate.TEMPLATE_B_PRICE_DROP;
        }
        // 4. Popular brands or high rating keywords in title -> Template E (Top-Rated)
        String titleLower = deal.getTitle() != null ? deal.getTitle().toLowerCase() : "";
        if (titleLower.contains("apple") || titleLower.contains("samsung") || titleLower.contains("sony") ||
            titleLower.contains("boat") || titleLower.contains("noise") || titleLower.contains("bestseller") ||
            titleLower.contains("pro") || titleLower.contains("rated")) {
            return ReelTemplate.TEMPLATE_E_TOP_RATED;
        }
        // 5. If MRP exists and is higher than price -> Template G (Comparison)
        if (deal.getMrp() != null && !deal.getMrp().equalsIgnoreCase("N/A") && savings > 0) {
            return ReelTemplate.TEMPLATE_G_COMPARISON;
        }
        // Default -> Template A (Product-Focused)
        return ReelTemplate.TEMPLATE_A_PRODUCT_FOCUSED;
    }

    public String generateHookForTemplate(Deal deal, ReelTemplate template) {
        if (deal == null) return "🔥 TODAY'S TOP DEAL ALERT";

        String priceStr = deal.getPrice() != null ? deal.getPrice().replaceAll("[^0-9.]", "") : "";
        String formattedPrice = priceStr.isEmpty() ? "" : "₹" + priceStr;
        String mrpStr = deal.getMrp() != null ? deal.getMrp().replaceAll("[^0-9.]", "") : "";
        String formattedMrp = mrpStr.isEmpty() ? "" : "₹" + mrpStr;
        int discount = deal.calculateDiscountPercent();
        String shortCategory = extractShortProductName(deal.getTitle());

        switch (template) {
            case TEMPLATE_A_PRODUCT_FOCUSED:
                if (!formattedPrice.isEmpty()) {
                    return "🔥 " + formattedPrice + " DEAL ALERT";
                }
                return "✨ THIS " + shortCategory.toUpperCase() + " IS A STEAL!";

            case TEMPLATE_B_PRICE_DROP:
                if (!formattedMrp.isEmpty() && !formattedPrice.isEmpty() && !formattedMrp.equals(formattedPrice)) {
                    return "⚡ " + formattedMrp + " ➔ " + formattedPrice + " PRICE CRASH!";
                }
                return "⚡ MASSIVE PRICE CRASH!";

            case TEMPLATE_C_BIG_DISCOUNT:
                if (discount > 0) {
                    return "🔥 FLAT " + discount + "% OFF — DON'T MISS THIS!";
                }
                return "🔥 MEGA DISCOUNT DEAL ALERT!";

            case TEMPLATE_D_UNDER_999:
                double pVal = 0;
                try { pVal = Double.parseDouble(priceStr); } catch (Exception ignored) {}
                if (pVal > 0 && pVal <= 500) {
                    return "🔥 CRAZY DEAL UNDER ₹500!";
                }
                return "🔥 BEST VALUE UNDER ₹999!";

            case TEMPLATE_E_TOP_RATED:
                return "⭐ 4.5+ STAR AMAZON BESTSELLER!";

            case TEMPLATE_F_FLASH_DEAL:
                return "⚡ 24-HOUR FLASH DEAL ALERT!";

            case TEMPLATE_G_COMPARISON:
                if (!formattedMrp.isEmpty() && !formattedPrice.isEmpty()) {
                    return "😱 " + formattedMrp + " VALUE FOR " + formattedPrice + " ONLY!";
                }
                return "💥 UNBEATABLE VALUE OFFER!";

            default:
                return "🔥 " + (formattedPrice.isEmpty() ? "SPECIAL" : formattedPrice) + " DEAL ALERT!";
        }
    }

    public String generateReelHookText(Deal deal, int formatIndex) {
        ReelTemplate template = determineTemplate(deal, formatIndex);
        return generateHookForTemplate(deal, template);
    }

    public String extractShortProductName(String title) {
        if (title == null || title.trim().isEmpty()) return "DEAL";
        String t = title.toLowerCase();

        if (t.contains("comforter") || t.contains("quilt") || t.contains("blanket")) return "Comforter Set";
        if (t.contains("earbuds") || t.contains("airpods") || t.contains("tws")) return "Earbuds";
        if (t.contains("headphone") || t.contains("headset")) return "Headphones";
        if (t.contains("watch") || t.contains("smartwatch")) return "Smartwatch";
        if (t.contains("shoe") || t.contains("sneaker")) return "Sneakers";
        if (t.contains("backpack") || t.contains("bag")) return "Backpack";
        if (t.contains("speaker")) return "Speaker";
        if (t.contains("curtain") || t.contains("bedsheet")) return "Home Linen";
        if (t.contains("trimmer") || t.contains("shaver")) return "Trimmer";
        if (t.contains("kettle") || t.contains("cooker")) return "Kitchen Tool";

        String[] words = title.split("[ ,|/\\-]+");
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String w : words) {
            if (!w.trim().isEmpty() && count < 2) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(w.trim());
                count++;
            }
        }
        return sb.length() > 0 ? sb.toString() : "Special Deal";
    }

    // =========================================================================
    // DYNAMIC BENEFITS EXTRACTION
    // =========================================================================

    public List<String> extractProductBenefits(Deal deal) {
        List<String> benefits = new ArrayList<>();
        if (deal == null || deal.getTitle() == null) {
            benefits.add("Top-Rated Customer Choice");
            benefits.add("Premium High Build Quality");
            benefits.add("Verified Amazon Seller");
            return benefits;
        }

        String t = deal.getTitle().toLowerCase();

        // 1. GSM / Bedding Keywords
        if (t.contains("gsm")) {
            for (String word : deal.getTitle().split("[ ,|]+")) {
                if (word.toUpperCase().contains("GSM")) {
                    benefits.add(word.toUpperCase() + " Ultra-Soft Comfort");
                    break;
                }
            }
        }

        if (t.contains("comforter") || t.contains("blanket") || t.contains("duvet") || t.contains("bedsheet")) {
            if (!benefits.contains("Soft & Breathable Fabric")) benefits.add("Soft & Breathable Microfiber");
            if (t.contains("double") || t.contains("king") || t.contains("queen")) {
                benefits.add("Double Bed / King Size Fit");
            } else {
                benefits.add("All-Season Lightweight Warmth");
            }
            benefits.add("Machine Washable & Durable");
        } else if (t.contains("earbuds") || t.contains("headphone") || t.contains("earphone") || t.contains("tws") || t.contains("audio")) {
            if (t.contains("anc") || t.contains("noise cancel")) {
                benefits.add("Active Noise Cancellation (ANC)");
            } else {
                benefits.add("Crystal Clear Audio & Deep Bass");
            }
            benefits.add("Long 30+ Hours Battery Life");
            benefits.add("Fast Type-C Quick Charging");
        } else if (t.contains("watch") || t.contains("smartwatch")) {
            benefits.add("Vibrant HD Display & BT Calling");
            benefits.add("24/7 Health & Fitness Tracking");
            benefits.add("Multi-Day Long Battery Life");
        } else if (t.contains("shoe") || t.contains("sneaker") || t.contains("sandal") || t.contains("footwear")) {
            benefits.add("Ultra-Comfort Cushioned Sole");
            benefits.add("Lightweight & Breathable");
            benefits.add("Durable Anti-Slip Grip");
        } else if (t.contains("shirt") || t.contains("t-shirt") || t.contains("kurta") || t.contains("pant") || t.contains("cotton")) {
            benefits.add("100% Breathable Soft Cotton");
            benefits.add("Modern Stylish Tailored Fit");
            benefits.add("Fade Resistant & Long Lasting");
        } else if (t.contains("kitchen") || t.contains("cooker") || t.contains("pan") || t.contains("bottle") || t.contains("kettle") || t.contains("steel")) {
            benefits.add("Premium Food-Grade Safe Material");
            benefits.add("Ergonomic & Easy to Clean");
            benefits.add("Durable Everyday Essential");
        } else if (t.contains("phone") || t.contains("mobile") || t.contains("laptop") || t.contains("tablet")) {
            benefits.add("High-Speed Fast Performance");
            benefits.add("Stunning Display & Camera");
            benefits.add("Official Brand Warranty");
        }

        if (benefits.size() < 1) benefits.add("⭐ Highly Rated by Customers");
        if (benefits.size() < 2) benefits.add("💎 Premium Build & Feel");
        if (benefits.size() < 3) benefits.add("📦 Fast & Verified Amazon Delivery");

        return benefits.subList(0, Math.min(3, benefits.size()));
    }

    // =========================================================================
    // 1:1 POST IMAGE GENERATION (FOR INSTAGRAM FEEDS & CAROUSELS)
    // =========================================================================

    public String createPostImage(Deal deal) throws Exception {
        return createPostImage(deal, "generated/post_image.jpg");
    }

    public String createPostImage(Deal deal, String outputPath) throws Exception {
        return createPostImage(
                deal.getImage(),
                deal.getTitle(),
                deal.getPrice(),
                deal.getMrp(),
                deal.calculateDiscountPercent(),
                deal.calculateSavingsAmount(),
                outputPath
        );
    }

    public String createPostImage(String imageUrl, String titleText, String priceText) throws Exception {
        return createPostImage(imageUrl, titleText, priceText, null, 0, 0, "generated/post_image.jpg");
    }

    public String createPostImage(String imageUrl, String titleText, String priceText, String mrpText, int discountPercent, long savingsAmount) throws Exception {
        return createPostImage(imageUrl, titleText, priceText, mrpText, discountPercent, savingsAmount, "generated/post_image.jpg");
    }

    public String createPostImage(String imageUrl, String titleText, String priceText, String mrpText, int discountPercent, long savingsAmount, String outputPath) throws Exception {
        File outputDir = new File("generated");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        BufferedImage canvas = renderPostImage(imageUrl, titleText, priceText, mrpText, discountPercent, savingsAmount);
        ImageIO.write(canvas, "jpg", new File(outputPath));
        System.out.println("Formatted HD post image created successfully: " + outputPath);
        return outputPath;
    }

    public BufferedImage renderPostImage(Deal deal) throws Exception {
        return renderPostImage(
                deal.getImage(),
                deal.getTitle(),
                deal.getPrice(),
                deal.getMrp(),
                deal.calculateDiscountPercent(),
                deal.calculateSavingsAmount()
        );
    }

    public BufferedImage renderPostImage(String imageUrl, String titleText, String priceText, String mrpText, int discountPercent, long savingsAmount) throws Exception {
        BufferedImage productImage = loadImage(imageUrl);

        // 1:1 Aspect Ratio Canvas (1080x1080)
        BufferedImage canvas = new BufferedImage(1080, 1080, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = canvas.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Modern Clean Normal Background (#F8FAFC to #E2E8F0)
        GradientPaint bgGradient = new GradientPaint(0, 0, new Color(248, 250, 252), 0, 1080, new Color(226, 232, 240));
        g.setPaint(bgGradient);
        g.fillRect(0, 0, 1080, 1080);

        // Top Banner Pill (Sapphire Indigo #2563EB to #4F46E5)
        int topPillX = 80;
        int topPillY = 28;
        int topPillW = 920;
        int topPillH = 68;

        GradientPaint topPillGradient = new GradientPaint(topPillX, topPillY, new Color(37, 99, 235), topPillX + topPillW, topPillY + topPillH, new Color(79, 70, 229));
        g.setPaint(topPillGradient);
        g.fillRoundRect(topPillX, topPillY, topPillW, topPillH, 34, 34);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 32));
        FontMetrics fmTop = g.getFontMetrics();
        String topText = "✨ TODAY'S CURATED SPECIAL DEAL ✨";
        int topX = (1080 - fmTop.stringWidth(topText)) / 2;
        g.drawString(topText, topX, topPillY + 46);

        // Large Hero Product Card (560x560)
        int cardSize = 560;
        int cardX = (1080 - cardSize) / 2;
        int cardY = 116;

        g.setColor(new Color(0, 0, 0, 20));
        g.fillRoundRect(cardX - 4, cardY + 8, cardSize + 8, cardSize + 8, 36, 36);

        g.setColor(Color.WHITE);
        g.fillRoundRect(cardX, cardY, cardSize, cardSize, 32, 32);
        g.setColor(new Color(226, 232, 240));
        g.setStroke(new BasicStroke(2.0f));
        g.drawRoundRect(cardX, cardY, cardSize, cardSize, 32, 32);

        if (productImage != null) {
            double scale = Math.min(
                    (double) (cardSize - 40) / productImage.getWidth(),
                    (double) (cardSize - 40) / productImage.getHeight()
            );

            int width = (int) (productImage.getWidth() * scale);
            int height = (int) (productImage.getHeight() * scale);

            Image scaled = productImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            int imgX = cardX + (cardSize - width) / 2;
            int imgY = cardY + (cardSize - height) / 2;
            g.drawImage(scaled, imgX, imgY, null);
        }

        // Discount Pill Badge (Top Left of Product Card)
        if (discountPercent > 0) {
            g.setColor(new Color(5, 150, 105)); // Emerald
            g.fillRoundRect(cardX + 16, cardY + 16, 210, 52, 16, 16);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 28));
            g.drawString("🏷️ " + discountPercent + "% OFF", cardX + 30, cardY + 52);
        }

        // Brand Badge (Top Right of Product Card)
        BrandBadge brand = detectBrandBadge(titleText);
        if (brand != null) {
            g.setFont(new Font("SansSerif", Font.BOLD, 24));
            FontMetrics fmBrand = g.getFontMetrics();
            int badgeW = fmBrand.stringWidth(brand.getName()) + 30;
            int badgeH = 50;
            int badgeX = (cardX + cardSize) - badgeW - 16;
            int badgeY = cardY + 16;

            g.setColor(new Color(15, 23, 42));
            g.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 16, 16);

            g.setColor(Color.WHITE);
            g.drawString(brand.getName(), badgeX + 15, badgeY + 34);
        }

        // Title Text Below Image (Max 2 lines)
        if (titleText != null && !titleText.isEmpty()) {
            g.setColor(new Color(15, 23, 42)); // Dark Slate
            g.setFont(new Font("SansSerif", Font.BOLD, 36));
            drawWrappedString(g, titleText, 80, 725, 920, 46, 2);
        }

        // Price Section (Pure White Card with Emerald Border)
        if (priceText != null && !priceText.isEmpty() && !priceText.equalsIgnoreCase("N/A")) {
            int boxX = 80;
            int boxY = 840;
            int boxW = 920;
            int boxH = 130;

            g.setColor(new Color(0, 0, 0, 20));
            g.fillRoundRect(boxX - 4, boxY + 6, boxW + 8, boxH + 6, 32, 32);

            g.setColor(Color.WHITE);
            g.fillRoundRect(boxX, boxY, boxW, boxH, 30, 30);

            g.setColor(new Color(16, 185, 129));
            g.setStroke(new BasicStroke(2.5f));
            g.drawRoundRect(boxX, boxY, boxW, boxH, 30, 30);

            String formattedOfferPrice = priceText.startsWith("₹") ? priceText : "₹" + priceText;
            String offerText = "DEAL: " + formattedOfferPrice;

            String mrpValStr = null;
            if (mrpText != null && !mrpText.isEmpty() && !mrpText.equalsIgnoreCase("N/A")) {
                mrpValStr = mrpText.startsWith("₹") ? mrpText : "₹" + mrpText;
            } else if (discountPercent > 0) {
                try {
                    double p = Double.parseDouble(priceText.replaceAll("[^0-9.]", ""));
                    long calcMrp = Math.round(p / (1.0 - (discountPercent / 100.0)));
                    if (calcMrp > p) {
                        mrpValStr = "₹" + String.format("%,d", calcMrp);
                    }
                } catch (Exception ignored) {}
            }

            if (mrpValStr != null && !mrpValStr.isEmpty()) {
                g.setFont(new Font("SansSerif", Font.BOLD, 36));
                FontMetrics fmMrpLabel = g.getFontMetrics();
                String mrpLabel = "MRP: ";
                String mrpFull = mrpLabel + mrpValStr;

                g.setFont(new Font("SansSerif", Font.BOLD, 52));
                FontMetrics fmOffer = g.getFontMetrics();

                int totalWidth = fmMrpLabel.stringWidth(mrpFull) + 40 + fmOffer.stringWidth(offerText);
                int startX = (1080 - totalWidth) / 2;

                g.setFont(new Font("SansSerif", Font.BOLD, 36));
                g.setColor(new Color(100, 116, 139));
                int mrpY = boxY + 78;
                g.drawString(mrpFull, startX, mrpY);

                int mrpValueStartX = startX + fmMrpLabel.stringWidth(mrpLabel);
                int mrpValueWidth = fmMrpLabel.stringWidth(mrpValStr);
                g.setColor(new Color(225, 29, 72));
                BasicStroke oldStroke = (BasicStroke) g.getStroke();
                g.setStroke(new BasicStroke(3.5f));
                g.drawLine(mrpValueStartX - 2, mrpY - 12, mrpValueStartX + mrpValueWidth + 2, mrpY - 12);
                g.setStroke(oldStroke);

                g.setFont(new Font("SansSerif", Font.BOLD, 52));
                g.setColor(new Color(5, 150, 105));
                g.drawString(offerText, startX + fmMrpLabel.stringWidth(mrpFull) + 40, mrpY + 2);
            } else {
                g.setColor(new Color(5, 150, 105));
                g.setFont(new Font("SansSerif", Font.BOLD, 54));
                FontMetrics fm = g.getFontMetrics();
                int textX = (1080 - fm.stringWidth(offerText)) / 2;
                g.drawString(offerText, textX, boxY + 82);
            }
        }

        // Clean Footer CTA
        g.setColor(new Color(100, 116, 139));
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        String handleText = "💬 Comment \"LINK\" or Link in Bio  •  Follow @offerzone2538";
        FontMetrics fmHandle = g.getFontMetrics();
        int handleX = (1080 - fmHandle.stringWidth(handleText)) / 2;
        g.drawString(handleText, handleX, 1030);

        g.dispose();
        return canvas;
    }

    // =========================================================================
    // AUDIO SYNTHESIS & ENVELOPES
    // =========================================================================

    private short[] generateMultiTrackAudioFrame(int themeIdx, int frameIndex, int totalFrames, int samplesPerFrame) {
        short[] buffer = new short[samplesPerFrame];
        double sampleRate = 44100.0;
        double totalDuration = (double) totalFrames / 30.0;

        switch (themeIdx % 6) {
            case 0: { // 1. ⚡ High-Energy Electro Tech House (128 BPM)
                double bpm = 128.0;
                double beatSec = 60.0 / bpm; // ~0.46875s
                int beatSamples = (int) (sampleRate * beatSec);
                int halfBeatSamples = beatSamples / 2;
                int sixteenthSamples = beatSamples / 4;

                double[] bassNotes = {87.31, 103.83, 116.54, 130.81, 103.83, 87.31};
                double[] leadNotes = {349.23, 415.30, 466.16, 523.25, 622.25, 698.46};

                for (int s = 0; s < samplesPerFrame; s++) {
                    int sampleIdx = frameIndex * samplesPerFrame + s;
                    double t = sampleIdx / sampleRate;

                    // 4-on-the-floor punchy kick
                    double beatT = (sampleIdx % beatSamples) / sampleRate;
                    double kickPitch = 55.0 + 80.0 * Math.exp(-beatT * 40.0);
                    double kick = Math.sin(2.0 * Math.PI * kickPitch * beatT) * Math.exp(-beatT * 18.0) * 0.35;

                    // Off-beat Open Hi-Hat
                    double offbeatT = ((sampleIdx + halfBeatSamples) % beatSamples) / sampleRate;
                    double hihatNoise = (Math.sin(sampleIdx * 1234.56) + Math.sin(sampleIdx * 4321.78)) * 0.5;
                    double hihat = hihatNoise * Math.exp(-offbeatT * 35.0) * 0.12;

                    // 16th-note bouncing rolling tech bassline (sidechained on kick)
                    int bassStep = (sampleIdx / sixteenthSamples) % bassNotes.length;
                    double bassFreq = bassNotes[bassStep];
                    double sixteenthT = (sampleIdx % sixteenthSamples) / sampleRate;
                    double sidechain = Math.min(1.0, beatT * 4.0);
                    double bass = (Math.sin(2.0 * Math.PI * bassFreq * t) * 0.22 
                            + Math.sin(4.0 * Math.PI * bassFreq * t) * 0.08) * sidechain;

                    // High lead synth arpeggio
                    int leadStep = (sampleIdx / (sixteenthSamples / 2)) % leadNotes.length;
                    double leadFreq = leadNotes[leadStep];
                    double lead = Math.sin(2.0 * Math.PI * leadFreq * t) * Math.exp(-sixteenthT * 12.0) * 0.14;

                    double mixed = (kick + hihat + bass + lead) * 0.42;
                    mixed *= applyEnvelope(t, totalDuration);
                    buffer[s] = (short) (Math.max(-1.0, Math.min(1.0, mixed)) * 32767);
                }
                break;
            }
            case 1: { // 2. 🔥 Viral Upbeat Pop & Slap Bass (126 BPM)
                double bpm = 126.0;
                double beatSec = 60.0 / bpm; // ~0.476s
                int beatSamples = (int) (sampleRate * beatSec);
                int halfBeatSamples = beatSamples / 2;

                double[] slapNotes = {55.00, 110.00, 65.41, 130.81, 73.42, 146.83, 82.41};
                double[] chordNotes = {440.00, 523.25, 659.25, 783.99};

                for (int s = 0; s < samplesPerFrame; s++) {
                    int sampleIdx = frameIndex * samplesPerFrame + s;
                    double t = sampleIdx / sampleRate;

                    // Kick on beats 1 & 3
                    double twoBeatT = (sampleIdx % (beatSamples * 2)) / sampleRate;
                    double kick1 = Math.sin(2.0 * Math.PI * (60.0 + 60.0 * Math.exp(-twoBeatT * 35.0)) * twoBeatT) 
                            * Math.exp(-twoBeatT * 16.0) * 0.32;

                    // Snappy Snare + Clap on beats 2 & 4
                    double snareT = ((sampleIdx + beatSamples) % (beatSamples * 2)) / sampleRate;
                    double snareBody = Math.sin(2.0 * Math.PI * 180.0 * snareT) * Math.exp(-snareT * 22.0) * 0.20;
                    double clapNoise = (Math.sin(sampleIdx * 789.12) + Math.sin(sampleIdx * 987.65)) * 0.5;
                    double clap = clapNoise * Math.exp(-snareT * 28.0) * 0.15;

                    // Bouncy Slap Bass Octave Bounce
                    int slapStep = (sampleIdx / halfBeatSamples) % slapNotes.length;
                    double bassFreq = slapNotes[slapStep];
                    double slapT = (sampleIdx % halfBeatSamples) / sampleRate;
                    double bass = (Math.sin(2.0 * Math.PI * bassFreq * t) * 0.25 
                            + Math.sin(3.0 * Math.PI * bassFreq * t) * 0.08) * Math.exp(-slapT * 9.0);

                    // Upbeat piano/synth chord stabs on off-beats
                    double offbeatT = ((sampleIdx + halfBeatSamples) % beatSamples) / sampleRate;
                    int chordStep = (sampleIdx / (beatSamples * 2)) % chordNotes.length;
                    double chord = (Math.sin(2.0 * Math.PI * chordNotes[chordStep] * t) 
                            + Math.sin(2.0 * Math.PI * chordNotes[(chordStep + 2) % chordNotes.length] * t) * 0.5) 
                            * Math.exp(-offbeatT * 16.0) * 0.16;

                    double mixed = (kick1 + snareBody + clap + bass + chord) * 0.40;
                    mixed *= applyEnvelope(t, totalDuration);
                    buffer[s] = (short) (Math.max(-1.0, Math.min(1.0, mixed)) * 32767);
                }
                break;
            }
            case 2: { // 3. 🚀 Dynamic Drum & Bass Fast Cadence (132 BPM)
                double bpm = 132.0;
                double beatSec = 60.0 / bpm; // ~0.4545s
                int beatSamples = (int) (sampleRate * beatSec);
                int eighthSamples = beatSamples / 2;

                double[] reeseNotes = {65.41, 65.41, 77.78, 87.31, 58.27};

                for (int s = 0; s < samplesPerFrame; s++) {
                    int sampleIdx = frameIndex * samplesPerFrame + s;
                    double t = sampleIdx / sampleRate;

                    // Fast DnB Breakbeat (Kick on 1 & 3.5, Snare on 2 & 4)
                    double beatInBar = (sampleIdx % (beatSamples * 4)) / (double) beatSamples;
                    double kickT = Math.min((sampleIdx % beatSamples) / sampleRate, 
                            ((sampleIdx + (int)(beatSamples * 0.5)) % beatSamples) / sampleRate);
                    double kick = (beatInBar < 0.25 || (beatInBar >= 2.5 && beatInBar < 2.75))
                            ? Math.sin(2.0 * Math.PI * 65.0 * kickT) * Math.exp(-kickT * 20.0) * 0.32
                            : 0.0;

                    double snareT = ((sampleIdx + beatSamples) % (beatSamples * 2)) / sampleRate;
                    double snare = Math.sin(2.0 * Math.PI * 220.0 * snareT) * Math.exp(-snareT * 24.0) * 0.25;

                    // Continuous 16th fast sizzle shaker
                    double shakerT = (sampleIdx % eighthSamples) / sampleRate;
                    double shaker = (Math.sin(sampleIdx * 543.21) * 0.5) * Math.exp(-shakerT * 40.0) * 0.10;

                    // Reese Sub Bass with filter modulation
                    int bassStep = (sampleIdx / (beatSamples * 2)) % reeseNotes.length;
                    double reeseFreq = reeseNotes[bassStep];
                    double lfo = Math.sin(2.0 * Math.PI * 4.0 * t);
                    double bass = (Math.sin(2.0 * Math.PI * reeseFreq * t) * 0.22 
                            + Math.sin(2.0 * Math.PI * (reeseFreq + 1.2) * t) * 0.15) * (0.8 + 0.2 * lfo);

                    double mixed = (kick + snare + shaker + bass) * 0.42;
                    mixed *= applyEnvelope(t, totalDuration);
                    buffer[s] = (short) (Math.max(-1.0, Math.min(1.0, mixed)) * 32767);
                }
                break;
            }
            case 3: { // 4. 💎 Modern Phonk & 808 Tech Drop (130 BPM)
                double bpm = 130.0;
                double beatSec = 60.0 / bpm; // ~0.4615s
                int beatSamples = (int) (sampleRate * beatSec);
                int sixteenthSamples = beatSamples / 4;

                double[] bellNotes = {587.33, 659.25, 783.99, 880.00, 783.99, 659.25};
                double[] subNotes = {65.41, 77.78, 87.31, 77.78};

                for (int s = 0; s < samplesPerFrame; s++) {
                    int sampleIdx = frameIndex * samplesPerFrame + s;
                    double t = sampleIdx / sampleRate;

                    // Heavy saturated 808 punch kick
                    double beatT = (sampleIdx % beatSamples) / sampleRate;
                    double kick = Math.tanh(Math.sin(2.0 * Math.PI * 52.0 * beatT) * 2.0) * Math.exp(-beatT * 14.0) * 0.32;

                    // Fast crisp Phonk cowbell / lead melody
                    int bellStep = (sampleIdx / (sixteenthSamples * 2)) % bellNotes.length;
                    double bellFreq = bellNotes[bellStep];
                    double bellT = (sampleIdx % (sixteenthSamples * 2)) / sampleRate;
                    double bell = (Math.sin(2.0 * Math.PI * bellFreq * t) * 0.22 
                            + Math.sin(4.0 * Math.PI * bellFreq * t) * 0.10) * Math.exp(-bellT * 15.0);

                    // Deep sliding 808 sub bass
                    int subStep = (sampleIdx / (beatSamples * 2)) % subNotes.length;
                    double subFreq = subNotes[subStep];
                    double sub = Math.sin(2.0 * Math.PI * subFreq * t) * 0.25;

                    // Fast hi-hat roll
                    double hatT = (sampleIdx % sixteenthSamples) / sampleRate;
                    double hat = Math.sin(sampleIdx * 888.8) * Math.exp(-hatT * 45.0) * 0.09;

                    double mixed = (kick + bell + sub + hat) * 0.40;
                    mixed *= applyEnvelope(t, totalDuration);
                    buffer[s] = (short) (Math.max(-1.0, Math.min(1.0, mixed)) * 32767);
                }
                break;
            }
            case 4: { // 5. ✨ Upbeat Commercial Tech House Groove (126 BPM)
                double bpm = 126.0;
                double beatSec = 60.0 / bpm; // ~0.476s
                int beatSamples = (int) (sampleRate * beatSec);
                int halfBeatSamples = beatSamples / 2;
                int sixteenthSamples = beatSamples / 4;

                double[] pluckNotes = {261.63, 311.13, 349.23, 392.00, 466.16, 523.25};

                for (int s = 0; s < samplesPerFrame; s++) {
                    int sampleIdx = frameIndex * samplesPerFrame + s;
                    double t = sampleIdx / sampleRate;

                    // Driving 4-on-the-floor kick
                    double beatT = (sampleIdx % beatSamples) / sampleRate;
                    double kick = Math.sin(2.0 * Math.PI * (58.0 + 70.0 * Math.exp(-beatT * 38.0)) * beatT) 
                            * Math.exp(-beatT * 15.0) * 0.34;

                    // Tech house open hi-hat on every offbeat
                    double offbeatT = ((sampleIdx + halfBeatSamples) % beatSamples) / sampleRate;
                    double hat = Math.sin(sampleIdx * 1111.11) * Math.exp(-offbeatT * 30.0) * 0.13;

                    // Bouncy synth pluck arpeggio
                    int pluckStep = (sampleIdx / sixteenthSamples) % pluckNotes.length;
                    double pluckFreq = pluckNotes[pluckStep];
                    double pluckT = (sampleIdx % sixteenthSamples) / sampleRate;
                    double pluck = (Math.sin(2.0 * Math.PI * pluckFreq * t) * 0.20 
                            + Math.sin(3.0 * Math.PI * pluckFreq * t) * 0.08) * Math.exp(-pluckT * 14.0);

                    // Pumping sub bass (sidechained)
                    double sidechain = Math.min(1.0, beatT * 3.5);
                    double bass = Math.sin(2.0 * Math.PI * 65.41 * t) * 0.22 * sidechain;

                    double mixed = (kick + hat + pluck + bass) * 0.42;
                    mixed *= applyEnvelope(t, totalDuration);
                    buffer[s] = (short) (Math.max(-1.0, Math.min(1.0, mixed)) * 32767);
                }
                break;
            }
            case 5:
            default: { // 6. 💥 Explosive Price-Crash Steal Beat (128 BPM)
                double bpm = 128.0;
                double beatSec = 60.0 / bpm; // ~0.46875s
                int beatSamples = (int) (sampleRate * beatSec);
                int eighthSamples = beatSamples / 2;

                double[] dropNotes = {440.00, 554.37, 659.25, 880.00};

                for (int s = 0; s < samplesPerFrame; s++) {
                    int sampleIdx = frameIndex * samplesPerFrame + s;
                    double t = sampleIdx / sampleRate;

                    // Phase 1 (0 to 8s - Hook & Showcase): Rising tension riser + fast ticks
                    // Phase 2 (8 to 17s - Price Drop & CTA): Explosive full drop with heavy sub and drive!
                    boolean isDropped = (t >= 8.0);

                    double beatT = (sampleIdx % beatSamples) / sampleRate;
                    double kick = 0.0;
                    double bass = 0.0;
                    double snare = 0.0;

                    if (!isDropped) {
                        // Rising Tension
                        double tensionPitch = 300.0 + (t / 8.0) * 400.0;
                        double riser = Math.sin(2.0 * Math.PI * tensionPitch * t) * 0.14;
                        double tickT = (sampleIdx % eighthSamples) / sampleRate;
                        double tick = Math.sin(sampleIdx * 999.0) * Math.exp(-tickT * 35.0) * 0.10;
                        kick = riser + tick;
                    } else {
                        // EXPLOSIVE DROP ON PRICE SLASH
                        kick = Math.sin(2.0 * Math.PI * (50.0 + 80.0 * Math.exp(-beatT * 35.0)) * beatT) 
                                * Math.exp(-beatT * 14.0) * 0.36;

                        double snareT = ((sampleIdx + beatSamples) % (beatSamples * 2)) / sampleRate;
                        snare = Math.sin(2.0 * Math.PI * 200.0 * snareT) * Math.exp(-snareT * 22.0) * 0.22;

                        int dropStep = (sampleIdx / eighthSamples) % dropNotes.length;
                        double dropFreq = dropNotes[dropStep];
                        bass = (Math.sin(2.0 * Math.PI * (dropFreq / 4.0) * t) * 0.26 
                                + Math.sin(2.0 * Math.PI * dropFreq * t) * 0.16);
                    }

                    double mixed = (kick + snare + bass) * 0.42;
                    mixed *= applyEnvelope(t, totalDuration);
                    buffer[s] = (short) (Math.max(-1.0, Math.min(1.0, mixed)) * 32767);
                }
                break;
            }
        }
        return buffer;
    }

    private double applyEnvelope(double currentTime, double totalDuration) {
        if (currentTime < 0.3) {
            return currentTime / 0.3;
        }
        if (currentTime > totalDuration - 0.4) {
            return Math.max(0.0, (totalDuration - currentTime) / 0.4);
        }
        return 1.0;
    }

    // =========================================================================
    // IMAGE LOADER & UTILITIES
    // =========================================================================

    private BufferedImage loadImage(String imageUrl) throws Exception {
        BufferedImage productImage = null;
        if (imageUrl != null && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
            String cleanUrl = imageUrl.trim().replaceAll("\\._[A-Za-z0-9_,-]+\\.(jpg|jpeg|png)", ".$1");
            if (!cleanUrl.contains("wsrv.nl") && (cleanUrl.contains("amazon.com") || cleanUrl.contains("amazon.in") || cleanUrl.contains("media-amazon.com"))) {
                cleanUrl = "https://wsrv.nl/?url=" + cleanUrl;
            }

            try {
                URL url = new URI(cleanUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                try (InputStream in = conn.getInputStream()) {
                    productImage = ImageIO.read(in);
                }
            } catch (Exception e) {
                System.out.println("⚠️ Warning: Could not download image from URL (" + cleanUrl + "): " + e.getMessage());
            }
        } else if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                productImage = ImageIO.read(new File(imageUrl));
            } catch (Exception e) {
                System.out.println("⚠️ Warning: Could not read image file (" + imageUrl + "): " + e.getMessage());
            }
        }

        if (productImage == null) {
            productImage = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = productImage.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 600, 600);
            g.setColor(Color.LIGHT_GRAY);
            g.setFont(new Font("SansSerif", Font.BOLD, 40));
            g.drawString("PRODUCT DEAL", 140, 310);
            g.dispose();
        }
        return productImage;
    }

    private void drawWrappedString(Graphics2D g, String text, int x, int startY, int maxWidth, int lineHeight, int maxLines) {
        FontMetrics fm = g.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        int y = startY;
        int lineCount = 0;

        for (String word : words) {
            if (fm.stringWidth(currentLine + " " + word) < maxWidth) {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            } else {
                g.drawString(currentLine.toString(), x, y);
                y += lineHeight;
                lineCount++;
                if (lineCount >= maxLines - 1) {
                    currentLine = new StringBuilder(word + "...");
                    break;
                }
                currentLine = new StringBuilder(word);
            }
        }
        if (currentLine.length() > 0 && lineCount < maxLines) {
            g.drawString(currentLine.toString(), x, y);
        }
    }

    public static class BrandBadge {
        private final String name;
        private final Color bgColor;
        private final Color textColor;

        public BrandBadge(String name, Color bgColor, Color textColor) {
            this.name = name;
            this.bgColor = bgColor;
            this.textColor = textColor;
        }

        public String getName() { return name; }
        public Color getBgColor() { return bgColor; }
        public Color getTextColor() { return textColor; }
    }

    public BrandBadge detectBrandBadge(String title) {
        if (title == null || title.trim().isEmpty()) return null;
        String t = title.toLowerCase();

        if (t.contains("apple") || t.contains("iphone") || t.contains("ipad") || t.contains("macbook") || t.contains("airpods")) {
            return new BrandBadge("🍎 APPLE", new Color(30, 41, 59), Color.WHITE);
        }
        if (t.contains("samsung") || t.contains("galaxy")) {
            return new BrandBadge("📱 SAMSUNG", new Color(29, 78, 216), Color.WHITE);
        }
        if (t.contains("boat") || t.contains("airdopes") || t.contains("bassheads") || t.contains("rockerz")) {
            return new BrandBadge("⚡ boAt", new Color(225, 29, 72), Color.WHITE);
        }
        if (t.contains("noise") || t.contains("colorfit")) {
            return new BrandBadge("🔥 NOISE", new Color(15, 23, 42), Color.WHITE);
        }
        if (t.contains("sony")) {
            return new BrandBadge("🎧 SONY", new Color(15, 23, 42), Color.WHITE);
        }
        if (t.contains("oneplus")) {
            return new BrandBadge("🔴 ONEPLUS", new Color(220, 38, 38), Color.WHITE);
        }
        if (t.contains("pigeon") || t.contains("prestige")) {
            return new BrandBadge("✨ PREMIUM", new Color(5, 150, 105), Color.WHITE);
        }
        if (t.contains("nike") || t.contains("puma") || t.contains("adidas")) {
            return new BrandBadge("⭐ OFFICIAL", new Color(15, 23, 42), Color.WHITE);
        }
        return null;
    }
}