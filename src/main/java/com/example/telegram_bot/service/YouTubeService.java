package com.example.telegram_bot.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.telegram_bot.model.Deal;
import com.example.telegram_bot.model.ProductCategory;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentSnippet;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadSnippet;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;

/**
 * Service to generate high-ranking YouTube Shorts packages and automatically
 * upload them to YouTube with auto-pinned affiliate link comments.
 */
@Service
public class YouTubeService {

    private final CategoryService categoryService;
    private final DealScoreService dealScoreService;
    private final GoogleSheetService googleSheetService;
    private final VideoGenerationService videoGenerationService;

    @Autowired(required = false)
    private YouTube youtubeClient;

    @Value("${app.server.base-url:http://localhost:8080}")
    private String serverBaseUrl = "http://localhost:8080";

    @Value("${telegram.chat.id:@BOnlinediscount}")
    private String telegramChannel = "@BOnlinediscount";

    @Autowired
    public YouTubeService(
            CategoryService categoryService,
            DealScoreService dealScoreService,
            GoogleSheetService googleSheetService,
            VideoGenerationService videoGenerationService) {
        this.categoryService = categoryService;
        this.dealScoreService = dealScoreService;
        this.googleSheetService = googleSheetService;
        this.videoGenerationService = videoGenerationService;
    }

    public YouTubeService(
            CategoryService categoryService,
            DealScoreService dealScoreService,
            GoogleSheetService googleSheetService,
            VideoGenerationService videoGenerationService,
            YouTube youtubeClient) {
        this.categoryService = categoryService;
        this.dealScoreService = dealScoreService;
        this.googleSheetService = googleSheetService;
        this.videoGenerationService = videoGenerationService;
        this.youtubeClient = youtubeClient;
    }

    /**
     * Generates a complete YouTube Shorts upload package for a given deal.
     */
    public Map<String, Object> generateShortsPackage(Deal deal) {
        Map<String, Object> pack = new LinkedHashMap<>();
        if (deal == null) return pack;

        ProductCategory category = categoryService.detectCategory(deal.getTitle());
        int discount = deal.calculateDiscountPercent();
        long savings = deal.calculateSavingsAmount();

        // 1. High-CTR Title (Max 100 characters for YouTube Shorts algorithm)
        String title = generateShortsTitle(deal, discount, category);

        // 2. High-Converting Description with Affiliate Disclosure
        String description = generateShortsDescription(deal, discount, savings, category);

        // 3. Pinned Comment (Holds direct purchase link & Telegram invite)
        String pinnedComment = generatePinnedComment(deal);

        // 4. High-Ranking Tags & Keywords
        List<String> tags = generateShortsTags(deal, category);

        // 5. Video File / Stream URL
        String videoStreamUrl = serverBaseUrl + "/video/stream";

        pack.put("status", "SUCCESS");
        pack.put("dealTitle", deal.getTitle());
        pack.put("price", deal.getPrice());
        pack.put("mrp", deal.getMrp());
        pack.put("discountPercent", discount + "%");
        pack.put("title", title);
        pack.put("description", description);
        pack.put("pinnedComment", pinnedComment);
        pack.put("tags", tags);
        pack.put("tagsCommaSeparated", String.join(", ", tags));
        pack.put("videoPath", "generated/reel.mp4");
        pack.put("videoStreamUrl", videoStreamUrl);
        pack.put("category", category.name());

        return pack;
    }

    /**
     * Picks the highest-scoring deal from Google Sheets, renders its 1080x1920 Reel,
     * and returns the complete ready-to-upload YouTube Shorts package.
     */
    public Map<String, Object> generateTopDealShortsPackage() throws Exception {
        Deal topDeal = fetchTopScoredDeal();
        if (topDeal == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", "NO_DEALS_FOUND");
            return err;
        }

        videoGenerationService.createReel(topDeal);
        return generateShortsPackage(topDeal);
    }

    /**
     * Automatically renders video reel and uploads YouTube Short for top deal from Google Sheet.
     */
    public Map<String, Object> uploadTopDealShortAutomatically() throws Exception {
        Deal topDeal = fetchTopScoredDeal();
        if (topDeal == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", "NO_DEALS_FOUND");
            err.put("message", "No valid deals found in Google Sheet to upload as YouTube Short.");
            return err;
        }

        return uploadShortAutomatically(topDeal);
    }

    /**
     * Automatically uploads a YouTube Short video and pins the direct affiliate comment.
     */
    public Map<String, Object> uploadShortAutomatically(Deal deal) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();

        if (youtubeClient == null) {
            result.put("status", "DISABLED");
            result.put("message", "YouTube client is not configured or disabled. Set YOUTUBE_CLIENT_ID, YOUTUBE_CLIENT_SECRET, and YOUTUBE_REFRESH_TOKEN in application.properties or environment variables.");
            return result;
        }

        if (deal == null) {
            result.put("status", "ERROR");
            result.put("message", "Deal cannot be null");
            return result;
        }

        // 1. Render Reel Video
        System.out.println("🎬 [YouTube Auto-Upload] Rendering 1080x1920 MP4 reel for: " + deal.getTitle());
        videoGenerationService.createReel(deal);
        File videoFile = new File("generated/reel.mp4");
        if (!videoFile.exists() || videoFile.length() == 0) {
            throw new IllegalStateException("Generated video file not found at " + videoFile.getAbsolutePath());
        }

        // 2. Prepare metadata
        Map<String, Object> pack = generateShortsPackage(deal);
        String title = (String) pack.get("title");
        String description = (String) pack.get("description");
        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) pack.get("tags");
        String pinnedCommentText = (String) pack.get("pinnedComment");

        Video video = new Video();

        // Video Status (Public)
        VideoStatus status = new VideoStatus();
        status.setPrivacyStatus("public");
        status.setSelfDeclaredMadeForKids(false);
        video.setStatus(status);

        // Video Snippet
        VideoSnippet snippet = new VideoSnippet();
        snippet.setTitle(title);
        snippet.setDescription(description);
        snippet.setTags(tags);
        snippet.setCategoryId("22"); // 22 = People & Blogs / Shopping
        video.setSnippet(snippet);

        // 3. Upload video via YouTube Data API v3
        System.out.println("🚀 [YouTube Auto-Upload] Uploading " + (videoFile.length() / 1024) + " KB video to YouTube channel...");
        InputStreamContent mediaContent = new InputStreamContent("video/mp4", new BufferedInputStream(new FileInputStream(videoFile)));
        mediaContent.setLength(videoFile.length());

        YouTube.Videos.Insert insertRequest = youtubeClient.videos()
                .insert(List.of("snippet", "status"), video, mediaContent);
        insertRequest.getMediaHttpUploader().setDirectUploadEnabled(false); // Enable resumable upload for reliability

        Video uploadedVideo = insertRequest.execute();
        String videoId = uploadedVideo.getId();
        String shortsUrl = "https://youtube.com/shorts/" + videoId;
        System.out.println("✅ [YouTube Auto-Upload] Uploaded successfully: " + shortsUrl);

        // 4. Post Affiliate Buy Link as Comment
        boolean commentPosted = false;
        try {
            postComment(videoId, pinnedCommentText);
            commentPosted = true;
            System.out.println("📌 [YouTube Auto-Upload] Affiliate comment posted on video ID: " + videoId);
        } catch (Exception commentEx) {
            System.err.println("⚠️ Could not post comment automatically: " + commentEx.getMessage());
        }

        result.put("status", "SUCCESS");
        result.put("videoId", videoId);
        result.put("shortsUrl", shortsUrl);
        result.put("title", title);
        result.put("affiliateCommentPosted", commentPosted);
        result.put("dealTitle", deal.getTitle());
        result.put("price", deal.getPrice());
        return result;
    }

    /**
     * Posts a top-level comment containing the buy link on the uploaded video.
     */
    public void postComment(String videoId, String commentText) throws Exception {
        if (youtubeClient == null || videoId == null || commentText == null) return;

        CommentSnippet commentSnippet = new CommentSnippet();
        commentSnippet.setTextOriginal(commentText);

        Comment topLevelComment = new Comment();
        topLevelComment.setSnippet(commentSnippet);

        CommentThreadSnippet threadSnippet = new CommentThreadSnippet();
        threadSnippet.setVideoId(videoId);
        threadSnippet.setTopLevelComment(topLevelComment);

        CommentThread thread = new CommentThread();
        thread.setSnippet(threadSnippet);

        youtubeClient.commentThreads()
                .insert(List.of("snippet"), thread)
                .execute();
    }

    /**
     * Checks YouTube API credentials and channel details.
     */
    public Map<String, Object> checkYouTubeStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        if (youtubeClient == null) {
            status.put("connected", false);
            status.put("message", "YouTube client is not initialized. Provide YOUTUBE_CLIENT_ID, YOUTUBE_CLIENT_SECRET, and YOUTUBE_REFRESH_TOKEN.");
            return status;
        }

        try {
            ChannelListResponse response = youtubeClient.channels()
                    .list(List.of("snippet", "statistics"))
                    .setMine(true)
                    .execute();

            if (response.getItems() != null && !response.getItems().isEmpty()) {
                var channel = response.getItems().get(0);
                status.put("connected", true);
                status.put("channelId", channel.getId());
                status.put("channelTitle", channel.getSnippet().getTitle());
                status.put("customUrl", channel.getSnippet().getCustomUrl());
                status.put("subscriberCount", channel.getStatistics() != null ? channel.getStatistics().getSubscriberCount() : "N/A");
                status.put("videoCount", channel.getStatistics() != null ? channel.getStatistics().getVideoCount() : "N/A");
            } else {
                status.put("connected", true);
                status.put("message", "YouTube API connected (No channel info returned)");
            }
        } catch (Exception e) {
            status.put("connected", false);
            status.put("error", e.getMessage());
        }
        return status;
    }

    private Deal fetchTopScoredDeal() throws Exception {
        if (googleSheetService == null) return null;
        List<List<Object>> rows = googleSheetService.getAllRows();
        if (rows == null || rows.isEmpty()) return null;

        List<Deal> candidates = new ArrayList<>();
        for (List<Object> row : rows) {
            String title = row.size() > 0 ? row.get(0).toString().trim() : "";
            String price = row.size() > 1 ? row.get(1).toString().trim() : "";
            String image = row.size() > 2 ? row.get(2).toString().trim() : "";
            String link = row.size() > 3 ? row.get(3).toString().trim() : "";
            String source = row.size() > 4 ? row.get(4).toString().trim() : "Amazon";

            if (title.isEmpty() || title.equalsIgnoreCase("Amazon Deal") || price.isEmpty() || price.equalsIgnoreCase("N/A") || image.isEmpty() || link.isEmpty()) {
                continue;
            }

            Deal d = new Deal(title, price, image, link, source);
            if (dealScoreService != null) {
                dealScoreService.scoreDeal(d);
            }
            candidates.add(d);
        }

        if (candidates.isEmpty()) return null;
        return (dealScoreService != null) ? dealScoreService.getTopRankedDeal(candidates) : candidates.get(0);
    }

    public String generateShortsTitle(Deal deal, int discount, ProductCategory category) {
        String cleanTitle = (videoGenerationService != null)
                ? videoGenerationService.extractShortProductName(deal.getTitle())
                : deal.getTitle();
        String hook;

        if (discount >= 60) {
            hook = "🔥 " + discount + "% OFF! " + cleanTitle + " Price Glitch?";
        } else if (discount >= 40) {
            hook = "⚡ HUGE PRICE DROP: " + cleanTitle + " (₹" + deal.getPrice() + ")";
        } else {
            hook = "👀 UNBOXING STEAL: " + cleanTitle + " on Amazon";
        }

        String titleWithTags = hook + " #shorts #deals #amazonfinds";
        if (titleWithTags.length() > 95) {
            return titleWithTags.substring(0, 95);
        }
        return titleWithTags;
    }

    public String generateShortsDescription(Deal deal, int discount, long savings, ProductCategory category) {
        StringBuilder sb = new StringBuilder();
        sb.append(deal.getTitle()).append("\n\n");
        sb.append("💰 Deal Price: ₹").append(deal.getPrice());
        if (deal.getMrp() != null && !deal.getMrp().trim().isEmpty() && !deal.getMrp().equalsIgnoreCase("N/A")) {
            sb.append(" (MRP: ₹").append(deal.getMrp()).append(")");
        }
        if (discount > 0) {
            sb.append("\n⚡ Verified Discount: ").append(discount).append("% OFF");
            if (savings > 0) {
                sb.append(" (Save ₹").append(String.format("%,d", savings)).append(")");
            }
        }
        sb.append("\n\n");

        if (deal.getLink() != null && !deal.getLink().trim().isEmpty()) {
            sb.append("🛒 BUY DIRECT ON AMAZON: 👇\n");
            sb.append(deal.getLink()).append("\n\n");
        }

        String tgHandle = (telegramChannel != null && !telegramChannel.trim().isEmpty())
                ? telegramChannel.replace("@", "")
                : "BOnlinediscount";
        sb.append("⚡ Join our Telegram Channel for instant 80% OFF price glitch alerts:\n");
        sb.append("👉 https://t.me/").append(tgHandle).append("\n\n");

        sb.append("❤️ Subscribe to OfferZone for daily secret Amazon price drops & unboxing finds!\n\n");
        sb.append("⚠️ Affiliate Disclosure: As an Amazon Associate, we earn from qualifying purchases at no extra cost to you.\n\n");

        sb.append("#amazonfinds #deals #techdeals #shoppinghacks #budgetdeals #amazonindia #unboxing");

        return sb.toString();
    }

    public String generatePinnedComment(Deal deal) {
        String tgHandle = (telegramChannel != null && !telegramChannel.trim().isEmpty())
                ? telegramChannel.replace("@", "")
                : "BOnlinediscount";
        return "🛒 Direct Purchase Link on Amazon: " + deal.getLink() + "\n" +
               "⚡ Join Telegram for instant 80% OFF loot alerts before deals expire: https://t.me/" + tgHandle;
    }

    public List<String> generateShortsTags(Deal deal, ProductCategory category) {
        List<String> tags = new ArrayList<>(Arrays.asList(
                "amazon finds",
                "amazon deals",
                "shorts",
                "deals india",
                "shopping hacks",
                "budget finds",
                "tech deals",
                "unboxing",
                "price drop",
                "best deals online",
                "offerzone"
        ));

        if (category != null && category != ProductCategory.DEFAULT) {
            tags.add(category.name().toLowerCase() + " deals");
            tags.add("best " + category.name().toLowerCase());
        }

        String titleLower = deal.getTitle() != null ? deal.getTitle().toLowerCase() : "";
        if (titleLower.contains("boat")) tags.add("boat deals");
        if (titleLower.contains("sony")) tags.add("sony headphones");
        if (titleLower.contains("noise")) tags.add("noise smartwatch");
        if (titleLower.contains("samsung")) tags.add("samsung deals");
        if (titleLower.contains("apple") || titleLower.contains("iphone")) tags.add("apple deals");

        return tags;
    }
}

