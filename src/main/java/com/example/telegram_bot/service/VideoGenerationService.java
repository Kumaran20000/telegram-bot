package com.example.telegram_bot.service;

import com.example.telegram_bot.model.Deal;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;

@Service
public class VideoGenerationService {

    public String createReel(Deal deal) throws Exception {
        return createReel(deal.getImage(), deal.getTitle(), deal.getPrice());
    }

    public String createReel(String imageUrl) throws Exception {
        return createReel(imageUrl, "🔥 DEAL OF THE DAY", "");
    }

    public String createReel(String imageUrl, String titleText, String priceText) throws Exception {

        File outputDir = new File("generated");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        String output = "generated/reel.mp4";

        // Load product image (supports HTTP/HTTPS URL or local file path)
        BufferedImage productImage = loadImage(imageUrl);

        // Create Reel canvas (1080x1920) - 9:16 Aspect Ratio
        BufferedImage canvas = new BufferedImage(1080, 1920, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();

        // Enable smooth rendering
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // White background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 1080, 1920);

        // Top Banner
        g.setColor(new Color(230, 57, 70)); // Vibrant Red
        g.fillRect(0, 0, 1080, 200);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 54));
        g.drawString("🔥 HOT DEAL ALERT 🔥", 220, 120);

        // Resize & Draw Product Image (Max 850x850)
        int maxWidth = 850;
        int maxHeight = 850;

        double scale = Math.min(
                (double) maxWidth / productImage.getWidth(),
                (double) maxHeight / productImage.getHeight()
        );

        int width = (int) (productImage.getWidth() * scale);
        int height = (int) (productImage.getHeight() * scale);

        Image scaled = productImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        int imgX = (1080 - width) / 2;
        int imgY = 240 + (maxHeight - height) / 2;

        // Subtle border around image
        g.setColor(new Color(240, 240, 240));
        g.fillRect(imgX - 10, imgY - 10, width + 20, height + 20);

        g.drawImage(scaled, imgX, imgY, null);

        // Title Text Below Image (Word Wrapped)
        if (titleText != null && !titleText.isEmpty()) {
            g.setColor(new Color(29, 53, 87));
            g.setFont(new Font("SansSerif", Font.BOLD, 46));
            drawWrappedString(g, titleText, 90, 1160, 900, 58, 4);
        }

        // Price Badge
        if (priceText != null && !priceText.isEmpty()) {
            g.setColor(new Color(42, 157, 143)); // Emerald Green
            g.fillRoundRect(140, 1480, 800, 140, 40, 40);

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 56));
            String formattedPrice = priceText.startsWith("₹") ? priceText : "PRICE: ₹" + priceText;
            FontMetrics fm = g.getFontMetrics();
            int textX = (1080 - fm.stringWidth(formattedPrice)) / 2;
            g.drawString(formattedPrice, textX, 1570);
        }

        // Bottom Footer
        g.setColor(new Color(241, 250, 238));
        g.fillRect(0, 1750, 1080, 170);

        g.setColor(new Color(29, 53, 87));
        g.setFont(new Font("SansSerif", Font.BOLD, 38));
        g.drawString("🛒 Buy Link in Bio | Telegram: @BOnlinediscount", 110, 1850);

        g.dispose();

        // Convert to video frame
        Java2DFrameConverter converter = new Java2DFrameConverter();
        Frame frame = converter.convert(canvas);

        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(output, 1080, 1920, 1);
        recorder.setFormat("mp4");
        recorder.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264);
        recorder.setPixelFormat(org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P);
        recorder.setFrameRate(30);
        recorder.setVideoBitrate(2_500_000);
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setOption("movflags", "+faststart");

        // Audio settings (Silent AAC track required by Instagram Reel video transcoders)
        recorder.setAudioCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_AAC);
        recorder.setAudioBitrate(128000);
        recorder.setSampleRate(44100);

        recorder.start();

        // 5-second video (150 frames @ 30 FPS) with silent AAC audio samples
        short[] silentAudio = new short[1470]; // 44100 / 30 = 1470 samples per frame
        for (int i = 0; i < 150; i++) {
            recorder.record(frame);
            recorder.recordSamples(java.nio.ShortBuffer.wrap(silentAudio));
        }

        recorder.stop();
        recorder.release();

        System.out.println("Reel created successfully: " + output);
        return output;
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

        // Background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 1080, 1080);

        // Top Banner
        g.setColor(new Color(230, 57, 70));
        g.fillRect(0, 0, 1080, 110);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 42));
        g.drawString("🔥 HOT DEAL ALERT 🔥", 300, 70);

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

        g.setColor(new Color(245, 245, 245));
        g.fillRect(imgX - 8, imgY - 8, width + 16, height + 16);
        g.drawImage(scaled, imgX, imgY, null);

        // Title Text Below Image
        if (titleText != null && !titleText.isEmpty()) {
            g.setColor(new Color(29, 53, 87));
            g.setFont(new Font("SansSerif", Font.BOLD, 36));
            drawWrappedString(g, titleText, 60, 770, 960, 44, 3);
        }

        // Price Badge
        if (priceText != null && !priceText.isEmpty()) {
            g.setColor(new Color(42, 157, 143));
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