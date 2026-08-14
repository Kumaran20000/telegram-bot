package com.example.telegram_bot.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Service;

import com.example.telegram_bot.model.Deal;

@Service
public class VideoGenerationService {

    public String createReel(Deal deal) throws Exception {
        return createReel(deal, 0);
    }

    public String createReel(Deal deal, int formatIndex) throws Exception {
        String hookBannerText = generateReelHookText(deal, formatIndex);
        return createReel(
                deal.getImage(),
                deal.getTitle(),
                deal.getPrice(),
                deal.getMrp(),
                deal.calculateDiscountPercent(),
                deal.calculateSavingsAmount(),
                hookBannerText
        );
    }

    public String createReel(String imageUrl) throws Exception {
        return createReel(imageUrl, "🔥 TODAY'S TOP DEAL", "", null, 0, 0, null);
    }

    public String createReel(String imageUrl, String titleText, String priceText) throws Exception {
        return createReel(imageUrl, titleText, priceText, null, 0, 0, null);
    }

    public String createReel(String imageUrl, String titleText, String priceText, String mrpText, int discountPercent, long savingsAmount) throws Exception {
        return createReel(imageUrl, titleText, priceText, mrpText, discountPercent, savingsAmount, null);
    }

    public String createReel(String imageUrl, String titleText, String priceText, String mrpText, int discountPercent, long savingsAmount, String hookBannerText) throws Exception {

        File outputDir = new File("generated");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        String output = "generated/reel.mp4";
        BufferedImage productImage = loadImage(imageUrl);

        Java2DFrameConverter converter = new Java2DFrameConverter();
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(output, 1080, 1920, 1);
        recorder.setFormat("mp4");
        recorder.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264);
        recorder.setPixelFormat(org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P);
        recorder.setFrameRate(30);
        recorder.setVideoBitrate(2_500_000);
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setOption("movflags", "+faststart");

        recorder.setAudioCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_AAC);
        recorder.setAudioBitrate(128000);
        recorder.setSampleRate(44100);

        recorder.start();

        int totalFrames = 150; // 5-second video (150 frames @ 30 FPS)
        for (int i = 0; i < totalFrames; i++) {
            double progress = (double) i / (double) totalFrames;
            // Ken Burns subtle zoom-in animation (100% to 108% scale)
            double zoomScale = 1.0 + (progress * 0.08);

            BufferedImage canvas = renderReelFrame(productImage, titleText, priceText, mrpText, discountPercent, savingsAmount, zoomScale, hookBannerText);
            Frame frame = converter.convert(canvas);
            recorder.record(frame);

            // Record energetic upbeat background music audio track (1470 samples @ 44.1kHz per 30fps frame)
            short[] audioBuffer = generateUpbeatAudioFrame(i, 1470);
            recorder.recordSamples(java.nio.ShortBuffer.wrap(audioBuffer));
        }

        recorder.stop();
        recorder.release();

        System.out.println("🎬 Dynamic 9:16 Animated Reel created successfully with Music & Hook [" + hookBannerText + "]: " + output);
        return output;
    }

    private short[] generateUpbeatAudioFrame(int frameIndex, int samplesPerFrame) {
        short[] buffer = new short[samplesPerFrame];
        double sampleRate = 44100.0;
        // Energetic synth chord progression (C4, G4, Am4, F4)
        double[] notes = {261.63, 329.63, 392.00, 523.25, 440.00, 349.23};
        int currentStep = (frameIndex / 6) % notes.length;
        double freq = notes[currentStep];

        for (int s = 0; s < samplesPerFrame; s++) {
            int sampleIdx = frameIndex * samplesPerFrame + s;
            double t = sampleIdx / sampleRate;

            // Melodic synth wave
            double synth = Math.sin(2.0 * Math.PI * freq * t) * 0.25;
            double oct = Math.sin(4.0 * Math.PI * (freq * 1.5) * t) * 0.15;

            // Rhythmic kick/bass pulse every 0.25s (120 BPM beat)
            double beatT = (sampleIdx % (int)(sampleRate * 0.25)) / sampleRate;
            double kick = Math.sin(2.0 * Math.PI * 65.0 * beatT) * Math.exp(-beatT * 18.0) * 0.45;

            // Mix together and clamp volume
            double mixed = (synth + oct + kick) * 0.45;
            buffer[s] = (short) (Math.max(-1.0, Math.min(1.0, mixed)) * 32767);
        }
        return buffer;
    }

    public String generateReelHookText(Deal deal, int formatIndex) {
        if (deal == null) return "🔥 TODAY'S #1 STEAL DEAL 🔥";

        int format = Math.abs(formatIndex) % 5;
        String price = deal.getPrice() != null ? deal.getPrice().replaceAll("[^0-9.]", "") : "";
        String mrp = deal.getMrp() != null ? deal.getMrp().replaceAll("[^0-9.]", "") : "";

        switch (format) {
            case 0:
                // Format 1: Budget Find (Under ₹500 or Under ₹1,000)
                double pVal = 0;
                try { pVal = Double.parseDouble(price); } catch (Exception ignored) {}
                if (pVal > 0 && pVal <= 500) {
                    return "🔥 AMAZON FIND UNDER ₹500 🔥";
                } else if (pVal > 0 && pVal <= 1000) {
                    return "🔥 AMAZON FIND UNDER ₹1,000 🔥";
                } else {
                    return "🔥 UNBEATABLE BUDGET FIND 🔥";
                }

            case 1:
                // Format 2: Extreme Price Slash (😱 ₹1,999 → ₹799)
                if (!mrp.isEmpty() && !price.isEmpty() && !mrp.equals(price)) {
                    return "😱 ₹" + mrp + " → ₹" + price + " 📉";
                } else if (deal.calculateDiscountPercent() > 0) {
                    return "😱 FLAT " + deal.calculateDiscountPercent() + "% PRICE DROP 🔥";
                } else {
                    return "😱 HUGE PRICE DROP STEAL 📉";
                }

            case 2:
                // Format 3: Target Audience / Student Hook
                return "🎓 MUST-HAVE FOR STUDENTS 👀";

            case 3:
                // Format 4: Viral Discovery Hook
                return "👀 I DIDN'T KNOW THIS EXISTED 🔥";

            case 4:
                // Format 5: Category / Useful Home Showcase Hook
                String titleLower = deal.getTitle() != null ? deal.getTitle().toLowerCase() : "";
                if (titleLower.contains("home") || titleLower.contains("kitchen") || titleLower.contains("bottle") || titleLower.contains("clean")) {
                    return "🏠 USEFUL PRODUCT FOR YOUR HOME 🏠";
                } else {
                    return "⚡ USEFUL DAILY ESSENTIAL FIND 🛍️";
                }

            default:
                return "🔥 TODAY'S #1 STEAL DEAL 🔥";
        }
    }

    private BufferedImage renderReelFrame(BufferedImage productImage, String titleText, String priceText, String mrpText, int discountPercent, long savingsAmount, double zoomScale, String hookBannerText) {
        BufferedImage canvas = new BufferedImage(1080, 1920, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. Sleek Modern Dark Gradient Background (#0F172A to #1E293B)
        GradientPaint bgGradient = new GradientPaint(0, 0, new Color(15, 23, 42), 0, 1920, new Color(30, 41, 59));
        g.setPaint(bgGradient);
        g.fillRect(0, 0, 1080, 1920);

        // 2. Sleek Modern Indigo-Violet Gradient Top Banner Pill (No Red Background)
        GradientPaint topPillGradient = new GradientPaint(90, 70, new Color(79, 70, 229), 990, 180, new Color(124, 58, 237));
        g.setPaint(topPillGradient);
        g.fillRoundRect(90, 70, 900, 110, 50, 50);

        // Gold Accent Border
        g.setColor(new Color(255, 214, 10, 180));
        g.setStroke(new java.awt.BasicStroke(3.0f));
        g.drawRoundRect(90, 70, 900, 110, 50, 50);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 44));
        FontMetrics fmTop = g.getFontMetrics();
        String topText = (hookBannerText != null && !hookBannerText.isEmpty()) ? hookBannerText : "🔥 TODAY'S #1 STEAL DEAL 🔥";
        int topX = (1080 - fmTop.stringWidth(topText)) / 2;
        g.drawString(topText, topX, 140);

        // 3. Product Card Frame with White Background & Rounded Corners
        int baseCardW = 860;
        int baseCardH = 860;
        int cardX = (1080 - baseCardW) / 2;
        int cardY = 220;

        // Card Drop Shadow
        g.setColor(new Color(0, 0, 0, 100));
        g.fillRoundRect(cardX - 10, cardY - 5, baseCardW + 20, baseCardH + 20, 40, 40);

        // Card Fill
        g.setColor(Color.WHITE);
        g.fillRoundRect(cardX, cardY, baseCardW, baseCardH, 40, 40);

        // 4. Product Image with Smooth Animated Ken Burns Zoom
        if (productImage != null) {
            double baseScale = Math.min(
                    (double) (baseCardW - 60) / productImage.getWidth(),
                    (double) (baseCardH - 60) / productImage.getHeight()
            );

            double finalScale = baseScale * zoomScale;
            int imgW = (int) (productImage.getWidth() * finalScale);
            int imgH = (int) (productImage.getHeight() * finalScale);

            Shape oldClip = g.getClip();
            g.setClip(new java.awt.geom.RoundRectangle2D.Double(cardX + 15, cardY + 15, baseCardW - 30, baseCardH - 30, 30, 30));

            int imgX = cardX + (baseCardW - imgW) / 2;
            int imgY = cardY + (baseCardH - imgH) / 2;

            g.drawImage(productImage.getScaledInstance(imgW, imgH, Image.SCALE_SMOOTH), imgX, imgY, null);
            g.setClip(oldClip);
        }

        // 5. Savings Badge Callout Overlay on Image (if discount > 0)
        if (discountPercent > 0 || savingsAmount > 0) {
            g.setColor(new Color(234, 88, 12)); // Bright Orange
            g.fillRoundRect(cardX + 30, cardY + 30, 360, 80, 30, 30);

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 36));
            String badgeText = discountPercent > 0 ? discountPercent + "% OFF" : "SAVE ₹" + savingsAmount;
            g.drawString("⚡ " + badgeText, cardX + 50, cardY + 83);
        }

        // Automatic Brand Badge Overlay (Top Right of Reel Product Card)
        BrandBadge brandReel = detectBrandBadge(titleText);
        if (brandReel != null) {
            g.setFont(new Font("SansSerif", Font.BOLD, 34));
            FontMetrics fmBrand = g.getFontMetrics();
            int badgeW = fmBrand.stringWidth(brandReel.getName()) + 40;
            int badgeH = 75;
            int badgeX = (cardX + baseCardW) - badgeW - 30;
            int badgeY = cardY + 30;

            g.setColor(brandReel.getBgColor());
            g.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 24, 24);

            g.setColor(brandReel.getTextColor());
            g.drawString(brandReel.getName(), badgeX + 20, badgeY + 50);
        }

        // 6. Title Text Below Card
        if (titleText != null && !titleText.isEmpty()) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 42));
            drawWrappedString(g, titleText, 90, 1140, 900, 54, 3);
        }

        // 7. Price Pill Badge with Original Price (MRP) & Offer Price
        if (priceText != null && !priceText.isEmpty() && !priceText.equalsIgnoreCase("N/A")) {
            g.setColor(new Color(16, 185, 129)); // Vibrant Emerald Green #10B981
            g.fillRoundRect(90, 1400, 900, 140, 50, 50);

            String formattedOfferPrice = priceText.startsWith("₹") ? priceText : "₹" + priceText;
            String offerText = "OFFER PRICE: " + formattedOfferPrice;

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
                FontMetrics fmMrp = g.getFontMetrics();
                String mrpLabel = "MRP: ";
                String mrpFull = mrpLabel + mrpValStr;

                g.setFont(new Font("SansSerif", Font.BOLD, 46));
                FontMetrics fmOffer = g.getFontMetrics();

                int totalW = fmMrp.stringWidth(mrpFull) + 40 + fmOffer.stringWidth(offerText);
                int startX = (1080 - totalW) / 2;

                // Draw MRP Label & Value
                g.setFont(new Font("SansSerif", Font.BOLD, 36));
                g.setColor(new Color(220, 252, 231)); // Soft Mint Light Text
                int mrpY = 1485;
                g.drawString(mrpFull, startX, mrpY);

                // Strikethrough line over MRP value
                int mrpValueX = startX + fmMrp.stringWidth(mrpLabel);
                int mrpValW = fmMrp.stringWidth(mrpValStr);
                g.setColor(new Color(239, 68, 68)); // Bright Red Strikethrough
                java.awt.Stroke oldStroke = g.getStroke();
                g.setStroke(new java.awt.BasicStroke(4.0f));
                g.drawLine(mrpValueX - 2, mrpY - 12, mrpValueX + mrpValW + 2, mrpY - 12);
                g.setStroke(oldStroke);

                // Draw OFFER PRICE
                g.setFont(new Font("SansSerif", Font.BOLD, 46));
                g.setColor(Color.WHITE);
                g.drawString(offerText, startX + fmMrp.stringWidth(mrpFull) + 40, mrpY + 2);
            } else {
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, 50));
                FontMetrics fmPrice = g.getFontMetrics();
                int px = (1080 - fmPrice.stringWidth(offerText)) / 2;
                g.drawString(offerText, px, 1488);
            }
        }

        // 8. Interactive Call-To-Action Pill at Bottom
        g.setColor(new Color(255, 214, 10)); // Gold Yellow
        g.fillRoundRect(90, 1600, 900, 110, 40, 40);

        g.setColor(new Color(15, 23, 42));
        g.setFont(new Font("SansSerif", Font.BOLD, 38));
        String ctaText = "💬 COMMENT \"LINK\" FOR DIRECT BUY LINK! 📩";
        FontMetrics fmCta = g.getFontMetrics();
        int ctaX = (1080 - fmCta.stringWidth(ctaText)) / 2;
        g.drawString(ctaText, ctaX, 1668);

        // 9. Account Handle Footer
        g.setColor(new Color(148, 163, 184)); // Slate Grey
        g.setFont(new Font("SansSerif", Font.BOLD, 34));
        String handleText = "❤️ Follow @offerzone2538 | Telegram: @BOnlinediscount";
        FontMetrics fmHandle = g.getFontMetrics();
        int handleX = (1080 - fmHandle.stringWidth(handleText)) / 2;
        g.drawString(handleText, handleX, 1820);

        g.dispose();
        return canvas;
    }

    // Generate a 1:1 (1080x1080) Square Image guaranteed to pass Instagram's aspect ratio requirements
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
        System.out.println("Formatted post image with Price & Offer Price created successfully: " + outputPath);
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

        // 1:1 Aspect Ratio Canvas (1080x1080) - Guaranteed compatible with Instagram Feed
        BufferedImage canvas = new BufferedImage(1080, 1080, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Dark gradient background
        GradientPaint bgGradient = new GradientPaint(0, 0, new Color(15, 23, 42), 0, 1080, new Color(30, 41, 59));
        g.setPaint(bgGradient);
        g.fillRect(0, 0, 1080, 1080);

        // Top Banner
        g.setColor(new Color(255, 56, 92));
        g.fillRoundRect(90, 30, 900, 80, 40, 40);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 38));
        FontMetrics fmTop = g.getFontMetrics();
        String topText = "🔥 TODAY'S SPECIAL OFFER 🔥";
        int topX = (1080 - fmTop.stringWidth(topText)) / 2;
        g.drawString(topText, topX, 82);

        // Resize Product Image (Max 520x520)
        int maxWidth = 520;
        int maxHeight = 520;

        double scale = Math.min(
                (double) maxWidth / productImage.getWidth(),
                (double) maxHeight / productImage.getHeight()
        );

        int width = (int) (productImage.getWidth() * scale);
        int height = (int) (productImage.getHeight() * scale);

        Image scaled = productImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        int imgX = (1080 - width) / 2;
        int imgY = 125 + (maxHeight - height) / 2;

        g.setColor(Color.WHITE);
        g.fillRoundRect(imgX - 10, imgY - 10, width + 20, height + 20, 20, 20);
        g.drawImage(scaled, imgX, imgY, null);

        // Discount Badge Callout Overlay
        if (discountPercent > 0) {
            g.setColor(new Color(234, 88, 12)); // Bright Orange
            g.fillRoundRect(imgX + 15, imgY + 15, 220, 60, 20, 20);

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 28));
            g.drawString("⚡ " + discountPercent + "% OFF", imgX + 30, imgY + 55);
        }

        // Automatic Brand Badge Overlay (Top Right of Product Card)
        BrandBadge brand = detectBrandBadge(titleText);
        if (brand != null) {
            g.setFont(new Font("SansSerif", Font.BOLD, 26));
            FontMetrics fmBrand = g.getFontMetrics();
            int badgeW = fmBrand.stringWidth(brand.getName()) + 32;
            int badgeH = 55;
            int badgeX = (imgX + width + 10) - badgeW - 15;
            int badgeY = imgY + 15;

            // Background Shadow
            g.setColor(new Color(0, 0, 0, 40));
            g.fillRoundRect(badgeX - 1, badgeY - 1, badgeW + 2, badgeH + 2, 18, 18);

            // Pill Background
            g.setColor(brand.getBgColor());
            g.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 18, 18);

            // Brand Text
            g.setColor(brand.getTextColor());
            g.drawString(brand.getName(), badgeX + 16, badgeY + 38);
        }

        // Title Text Below Image
        if (titleText != null && !titleText.isEmpty()) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 34));
            drawWrappedString(g, titleText, 60, 695, 960, 42, 2);
        }

        // Price Badge Section with Price (MRP / Original Price) and Offer Price
        if (priceText != null && !priceText.isEmpty() && !priceText.equalsIgnoreCase("N/A")) {
            int boxX = 60;
            int boxY = 870;
            int boxW = 960;
            int boxH = 140;

            // Sleek Rounded Emerald Card Background
            g.setColor(new Color(16, 185, 129)); // #10B981 Emerald Green
            g.fillRoundRect(boxX, boxY, boxW, boxH, 40, 40);

            String formattedOfferPrice = priceText.startsWith("₹") ? priceText : "₹" + priceText;
            String offerText = "OFFER PRICE: " + formattedOfferPrice;

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

                g.setFont(new Font("SansSerif", Font.BOLD, 46));
                FontMetrics fmOffer = g.getFontMetrics();

                int totalWidth = fmMrpLabel.stringWidth(mrpFull) + 40 + fmOffer.stringWidth(offerText);
                int startX = (1080 - totalWidth) / 2;

                // Draw MRP Label & Value
                g.setFont(new Font("SansSerif", Font.BOLD, 36));
                g.setColor(new Color(220, 252, 231)); // Soft Mint Light Text
                int mrpY = boxY + 85;
                g.drawString(mrpFull, startX, mrpY);

                // Draw Red Strikethrough Line across MRP Value
                int mrpValueStartX = startX + fmMrpLabel.stringWidth(mrpLabel);
                int mrpValueWidth = fmMrpLabel.stringWidth(mrpValStr);
                g.setColor(new Color(239, 68, 68)); // Bright Red Strikethrough
                java.awt.Stroke oldStroke = g.getStroke();
                g.setStroke(new java.awt.BasicStroke(4.0f));
                g.drawLine(mrpValueStartX - 2, mrpY - 12, mrpValueStartX + mrpValueWidth + 2, mrpY - 12);
                g.setStroke(oldStroke);

                // Draw Offer Price
                g.setFont(new Font("SansSerif", Font.BOLD, 46));
                g.setColor(Color.WHITE);
                g.drawString(offerText, startX + fmMrpLabel.stringWidth(mrpFull) + 40, mrpY + 2);
            } else {
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, 48));
                FontMetrics fm = g.getFontMetrics();
                int textX = (1080 - fm.stringWidth(offerText)) / 2;
                g.drawString(offerText, textX, boxY + 88);
            }
        }

        // Account Handle Footer
        g.setColor(new Color(148, 163, 184)); // Slate Grey
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        String handleText = "❤️ Follow @offerzone2538 | Telegram: @BOnlinediscount";
        FontMetrics fmHandle = g.getFontMetrics();
        int handleX = (1080 - fmHandle.stringWidth(handleText)) / 2;
        g.drawString(handleText, handleX, 1045);

        g.dispose();
        return canvas;
    }

    private BufferedImage loadImage(String imageUrl) throws Exception {
        BufferedImage productImage = null;
        if (imageUrl != null && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
            try {
                java.net.URL url = new java.net.URI(imageUrl).toURL();
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                try (java.io.InputStream in = conn.getInputStream()) {
                    productImage = ImageIO.read(in);
                }
            } catch (Exception e) {
                System.out.println("⚠️ Warning: Could not download image from URL (" + imageUrl + "): " + e.getMessage());
            }
        } else if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                productImage = ImageIO.read(new File(imageUrl));
            } catch (Exception e) {
                System.out.println("⚠️ Warning: Could not read image file (" + imageUrl + "): " + e.getMessage());
            }
        }

        if (productImage == null) {
            System.out.println("⚠️ Using blank fallback image template for deal video/post.");
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

    public BrandBadge detectBrandBadge(String titleText) {
        if (titleText == null || titleText.trim().isEmpty()) return null;
        String lower = titleText.toLowerCase();

        if (lower.contains("apple") || lower.contains("iphone") || lower.contains("macbook") || lower.contains("ipad") || lower.contains("airpods")) {
            return new BrandBadge("🍎 APPLE", new Color(15, 23, 42), Color.WHITE);
        } else if (lower.contains("samsung") || lower.contains("galaxy")) {
            return new BrandBadge("📱 SAMSUNG", new Color(3, 78, 162), Color.WHITE);
        } else if (lower.contains("sony") || lower.contains("playstation") || lower.contains("bravia")) {
            return new BrandBadge("🎧 SONY", new Color(0, 0, 0), Color.WHITE);
        } else if (lower.contains("boat") || lower.contains("airdopes") || lower.contains("rockerz")) {
            return new BrandBadge("⚡ boAt", new Color(225, 25, 50), Color.WHITE);
        } else if (lower.contains("noise") || lower.contains("colorfit")) {
            return new BrandBadge("🔥 NOISE", new Color(0, 102, 255), Color.WHITE);
        } else if (lower.contains("oneplus") || lower.contains("nord")) {
            return new BrandBadge("⚡ ONEPLUS", new Color(240, 0, 0), Color.WHITE);
        } else if (lower.contains("realme")) {
            return new BrandBadge("🟡 REALME", new Color(255, 199, 0), new Color(15, 23, 42));
        } else if (lower.contains("redmi") || lower.contains("xiaomi") || lower.contains("mi ")) {
            return new BrandBadge("🍊 XIAOMI", new Color(255, 103, 0), Color.WHITE);
        } else if (lower.contains("nike")) {
            return new BrandBadge("✔️ NIKE", new Color(17, 17, 17), Color.WHITE);
        } else if (lower.contains("adidas")) {
            return new BrandBadge("👟 ADIDAS", new Color(15, 23, 42), Color.WHITE);
        } else if (lower.contains("puma")) {
            return new BrandBadge("🐾 PUMA", new Color(186, 32, 38), Color.WHITE);
        } else if (lower.contains("dell")) {
            return new BrandBadge("💻 DELL", new Color(0, 118, 206), Color.WHITE);
        } else if (lower.contains("hp") || lower.contains("pavilion") || lower.contains("victus")) {
            return new BrandBadge("💻 HP", new Color(0, 150, 214), Color.WHITE);
        } else if (lower.contains("asus") || lower.contains("rog") || lower.contains("tuf")) {
            return new BrandBadge("🎮 ASUS", new Color(0, 83, 155), Color.WHITE);
        } else if (lower.contains("lenovo") || lower.contains("ideapad") || lower.contains("thinkpad")) {
            return new BrandBadge("💻 LENOVO", new Color(226, 35, 26), Color.WHITE);
        } else if (lower.contains("ptron")) {
            return new BrandBadge("⚡ pTron", new Color(220, 38, 38), Color.WHITE);
        } else if (lower.contains("jbl")) {
            return new BrandBadge("🔊 JBL", new Color(255, 102, 0), Color.WHITE);
        } else if (lower.contains("zebronics")) {
            return new BrandBadge("🔊 ZEBRONICS", new Color(37, 99, 235), Color.WHITE);
        } else if (lower.contains("boult")) {
            return new BrandBadge("⚡ BOUULT", new Color(124, 58, 237), Color.WHITE);
        } else if (lower.contains("fastrack")) {
            return new BrandBadge("⌚ FASTRACK", new Color(234, 88, 12), Color.WHITE);
        } else if (lower.contains("fire-boltt") || lower.contains("fireboltt")) {
            return new BrandBadge("⚡ FIRE-BOLTT", new Color(220, 38, 38), Color.WHITE);
        } else if (lower.contains("lg")) {
            return new BrandBadge("📺 LG", new Color(165, 0, 52), Color.WHITE);
        } else if (lower.contains("logitech")) {
            return new BrandBadge("🖱️ LOGITECH", new Color(0, 184, 252), Color.WHITE);
        } else if (lower.contains("canon")) {
            return new BrandBadge("📷 CANON", new Color(204, 0, 0), Color.WHITE);
        } else if (lower.contains("casio") || lower.contains("g-shock") || lower.contains("gshock")) {
            return new BrandBadge("⌚ CASIO", new Color(0, 51, 153), Color.WHITE);
        }
        return null;
    }
}