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
        return createReel(
                deal.getImage(),
                deal.getTitle(),
                deal.getPrice(),
                deal.getMrp(),
                deal.calculateDiscountPercent(),
                deal.calculateSavingsAmount()
        );
    }

    public String createReel(String imageUrl) throws Exception {
        return createReel(imageUrl, "🔥 TODAY'S TOP DEAL", "", null, 0, 0);
    }

    public String createReel(String imageUrl, String titleText, String priceText) throws Exception {
        return createReel(imageUrl, titleText, priceText, null, 0, 0);
    }

    /**
     * Generates a 9:16 vertical 1080x1920 Reel video with smooth Ken Burns animated zoom motion,
     * dark gradient glassmorphism UI, savings badges, and clear comment CTAs for maximum engagement.
     */
    public String createReel(String imageUrl, String titleText, String priceText, String mrpText, int discountPercent, long savingsAmount) throws Exception {

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

        short[] silentAudio = new short[1470]; // 44100 / 30 = 1470 samples per frame

        int totalFrames = 150; // 5-second video (150 frames @ 30 FPS)
        for (int i = 0; i < totalFrames; i++) {
            double progress = (double) i / (double) totalFrames;
            // Ken Burns subtle zoom-in animation (100% to 108% scale)
            double zoomScale = 1.0 + (progress * 0.08);

            BufferedImage canvas = renderReelFrame(productImage, titleText, priceText, mrpText, discountPercent, savingsAmount, zoomScale);
            Frame frame = converter.convert(canvas);
            recorder.record(frame);
            recorder.recordSamples(java.nio.ShortBuffer.wrap(silentAudio));
        }

        recorder.stop();
        recorder.release();

        System.out.println("🎬 Dynamic 9:16 Animated Reel created successfully: " + output);
        return output;
    }

    private BufferedImage renderReelFrame(BufferedImage productImage, String titleText, String priceText, String mrpText, int discountPercent, long savingsAmount, double zoomScale) {
        BufferedImage canvas = new BufferedImage(1080, 1920, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. Sleek Modern Dark Gradient Background (#0F172A to #1E293B)
        GradientPaint bgGradient = new GradientPaint(0, 0, new Color(15, 23, 42), 0, 1920, new Color(30, 41, 59));
        g.setPaint(bgGradient);
        g.fillRect(0, 0, 1080, 1920);

        // 2. Top Banner Pill Badge (#FF385C Red Coral)
        g.setColor(new Color(255, 56, 92));
        g.fillRoundRect(90, 70, 900, 110, 50, 50);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 46));
        FontMetrics fmTop = g.getFontMetrics();
        String topText = "🔥 TODAY'S #1 STEAL DEAL 🔥";
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

        // 6. Title Text Below Card
        if (titleText != null && !titleText.isEmpty()) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 42));
            drawWrappedString(g, titleText, 90, 1140, 900, 54, 3);
        }

        // 7. Price Pill Badge (Vibrant Emerald Green #10B981)
        if (priceText != null && !priceText.isEmpty()) {
            g.setColor(new Color(16, 185, 129));
            g.fillRoundRect(90, 1400, 900, 140, 50, 50);

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 52));

            String formattedPrice = priceText.startsWith("₹") ? priceText : "₹" + priceText;
            String priceLine = "PRICE: " + formattedPrice;
            if (mrpText != null && !mrpText.isEmpty() && !mrpText.equalsIgnoreCase("N/A")) {
                priceLine += "  (MRP ₹" + mrpText + ")";
            }

            FontMetrics fmPrice = g.getFontMetrics();
            int px = (1080 - fmPrice.stringWidth(priceLine)) / 2;
            g.drawString(priceLine, px, 1488);
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
        return createPostImage(deal.getImage(), deal.getTitle(), deal.getPrice());
    }

    public String createPostImage(String imageUrl, String titleText, String priceText) throws Exception {
        File outputDir = new File("generated");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        String outputPath = "generated/post_image.jpg";
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
        g.fillRoundRect(90, 30, 900, 90, 40, 40);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 42));
        FontMetrics fmTop = g.getFontMetrics();
        String topText = "🔥 HOT DEAL ALERT 🔥";
        int topX = (1080 - fmTop.stringWidth(topText)) / 2;
        g.drawString(topText, topX, 90);

        // Resize Product Image (Max 600x600)
        int maxWidth = 600;
        int maxHeight = 600;

        double scale = Math.min(
                (double) maxWidth / productImage.getWidth(),
                (double) maxHeight / productImage.getHeight()
        );

        int width = (int) (productImage.getWidth() * scale);
        int height = (int) (productImage.getHeight() * scale);

        Image scaled = productImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        int imgX = (1080 - width) / 2;
        int imgY = 130 + (maxHeight - height) / 2;

        g.setColor(Color.WHITE);
        g.fillRoundRect(imgX - 10, imgY - 10, width + 20, height + 20, 20, 20);
        g.drawImage(scaled, imgX, imgY, null);

        // Title Text Below Image
        if (titleText != null && !titleText.isEmpty()) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 36));
            drawWrappedString(g, titleText, 60, 770, 960, 44, 3);
        }

        // Price Badge
        if (priceText != null && !priceText.isEmpty()) {
            g.setColor(new Color(16, 185, 129));
            g.fillRoundRect(140, 920, 800, 110, 30, 30);

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 46));
            String formattedPrice = priceText.startsWith("₹") ? priceText : "PRICE: ₹" + priceText;
            FontMetrics fm = g.getFontMetrics();
            int textX = (1080 - fm.stringWidth(formattedPrice)) / 2;
            g.drawString(formattedPrice, textX, 990);
        }

        g.dispose();

        ImageIO.write(canvas, "jpg", new File(outputPath));
        System.out.println("Formatted post image created successfully: " + outputPath);
        return outputPath;
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
}