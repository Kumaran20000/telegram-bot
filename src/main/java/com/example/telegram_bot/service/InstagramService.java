package com.example.telegram_bot.service;

import com.example.telegram_bot.config.InstagramConfig;
import com.example.telegram_bot.model.Deal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InstagramService {

    private final RestTemplate restTemplate;
    private final InstagramConfig instagramConfig;
    private final CaptionService captionService;
    private final VideoGenerationService videoGenerationService;

    @org.springframework.beans.factory.annotation.Value("${app.server.base-url:http://localhost:8080}")
    private String serverBaseUrl;

    // Main method to publish static image post using deal's formatted image URL (with Price & Offer Price overlay)
    public boolean publish(Deal deal) {
        if (deal == null) return false;
        try {
            if (videoGenerationService != null) {
                videoGenerationService.createPostImage(deal);
            }
            boolean isLocalServer = serverBaseUrl == null || serverBaseUrl.contains("localhost") || serverBaseUrl.contains("127.0.0.1");
            if (!isLocalServer) {
                String formattedImageUrl = serverBaseUrl + "/video/image/stream";
                System.out.println("Publishing 1:1 post image with Price & Offer Price overlay: " + formattedImageUrl);
                boolean success = publish(deal, formattedImageUrl);
                if (success) return true;
            } else {
                System.out.println("⚠️ Warning: 'app.server.base-url' is set to localhost (" + serverBaseUrl + ").");
                System.out.println("⚠️ Meta Instagram API cannot pull local images directly from localhost. Run 'ngrok http 8080' or deploy to Render to send price overlay images.");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Warning: Could not pre-generate 1:1 post image with price overlay: " + e.getMessage());
        }
        return publish(deal, deal.getImage());
    }

    // Overloaded method to publish static image post using custom image URL (e.g. formatted 1:1 image)
    public boolean publish(Deal deal, String imageUrl) {
        System.out.println("Business ID: " + instagramConfig.getBusinessId());
        System.out.println("Access Token: " + instagramConfig.getAccessToken());

        try {
            String targetImageUrl = getProxiedImageUrl(imageUrl);
            System.out.println("Starting Instagram Image upload with URL: " + targetImageUrl);

            if (!isValidInstagramAspectRatio(targetImageUrl)) {
                System.out.println("❌ Skipping Instagram upload: Image aspect ratio is not supported by Instagram.");
                return false;
            }

            String creationId = createMediaContainer(deal, targetImageUrl);

            if (creationId == null) {
                System.out.println("Failed to create Instagram media container");
                return false;
            }

            System.out.println("Creation ID: " + creationId);
            boolean published = publishMedia(creationId);

            if (published) {
                System.out.println("Instagram post published successfully");
            } else {
                System.out.println("Instagram publish failed");
            }

            return published;

        } catch (Exception e) {
            System.out.println("Instagram Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public String cleanAmazonImageUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return "https://dummyimage.com/600x600/ffffff/000000.jpg&text=Amazon+Deal";
        }
        String cleaned = rawUrl.trim();
        // Remove dynamic sizing modifiers like ._SL1500_ ._AC_UL320_ ._SX..._ ._SY..._
        cleaned = cleaned.replaceAll("\\._[A-Za-z0-9_,-]+\\.(jpg|jpeg|png)", ".$1");
        return cleaned;
    }

    /**
     * Cleans dynamic size modifiers and proxies Amazon product image URLs through wsrv.nl to bypass CloudFront User-Agent blocking of Meta's Instagram crawler.
     */
    public String getProxiedImageUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return "https://dummyimage.com/600x600/ffffff/000000.jpg&text=Amazon+Deal";
        }
        String cleanUrl = cleanAmazonImageUrl(rawUrl);
        if (cleanUrl.contains("dummyimage.com") || cleanUrl.contains("wsrv.nl") || cleanUrl.contains("weserv.nl")) {
            return cleanUrl;
        }
        if (cleanUrl.contains("amazon.com") || cleanUrl.contains("amazon.in") || cleanUrl.contains("media-amazon.com") || cleanUrl.contains("ssl-images-amazon")) {
            return "https://wsrv.nl/?url=" + cleanUrl;
        }
        return cleanUrl;
    }

    /**
     * Validates if an image URL has an aspect ratio compatible with Instagram Feed posts.
     * Instagram Graph API requires image aspect ratios to be between 4:5 (0.80) and 1.91:1 (1.91).
     * If local inspection fails or image CDN blocks default Java client, passes through to Meta API.
     */
    public boolean isValidInstagramAspectRatio(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return false;
        }
        try {
            java.net.URL url = new java.net.URL(imageUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(conn.getInputStream());
            if (img == null) {
                System.out.println("⚠️ Could not read image dimensions locally, passing through to Meta Instagram API: " + imageUrl);
                return true;
            }
            double width = img.getWidth();
            double height = img.getHeight();
            if (height <= 0) {
                return true;
            }
            double aspectRatio = width / height;
            System.out.printf("Image Aspect Ratio check for [%s]: Dimensions=%.0fx%.0f, Aspect Ratio=%.3f%n",
                    imageUrl, width, height, aspectRatio);

            if (aspectRatio < 0.80 || aspectRatio > 1.91) {
                System.out.printf("⚠️ Image aspect ratio %.3f is outside standard 0.80-1.91. Attempting Meta API upload regardless.%n", aspectRatio);
            }
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Warning during image aspect ratio check (" + e.getMessage() + "), passing through to Meta Instagram API: " + imageUrl);
            return true;
        }
    }

    public boolean publishReel(Deal deal) {
        return publishReel(deal, null);
    }

    // Method to publish an Instagram Reel (Video)
    public boolean publishReel(Deal deal, String videoUrl) {
        System.out.println("🎬 Starting Instagram Reel publish workflow...");
        try {
            File reelFile = new File("generated/reel.mp4");
            if (!reelFile.exists() || reelFile.length() == 0) {
                System.out.println("Generating Reel video for deal: " + deal.getTitle());
                if (videoGenerationService != null) {
                    videoGenerationService.createReel(deal);
                }
                reelFile = new File("generated/reel.mp4");
            }

            String creationId = null;

            // Strategy 1: Direct Resumable Upload (Works from localhost & cloud without public tunnels!)
            if (reelFile.exists() && reelFile.length() > 0) {
                System.out.println("🚀 Attempting Direct Resumable Upload of video bytes to Meta (" + (reelFile.length() / 1024) + " KB)...");
                creationId = createReelViaDirectUpload(deal, reelFile);
            }

            // Strategy 2: URL-based container creation (if public URL is provided and direct upload did not succeed)
            if (creationId == null && videoUrl != null && !videoUrl.contains("localhost") && !videoUrl.contains("127.0.0.1")) {
                System.out.println("Attempting URL-based Reel Container Creation via URL: " + videoUrl);
                creationId = createReelMediaContainer(deal, videoUrl);
            }

            if (creationId == null) {
                System.out.println("❌ Failed to create Instagram Reel container via direct upload or URL.");
                return false;
            }

            System.out.println("Reel Creation ID: " + creationId + ". Waiting for Meta video processing...");

            // Wait for video processing on Meta servers
            boolean ready = waitForMediaContainerProcessing(creationId);
            if (!ready) {
                System.out.println("❌ Reel video processing timed out or failed on Meta servers.");
                return false;
            }

            boolean published = publishMedia(creationId);
            if (published) {
                System.out.println("✅ Instagram Reel published successfully!");
            } else {
                System.out.println("❌ Instagram Reel publish failed.");
            }
            return published;

        } catch (Exception e) {
            System.out.println("❌ Instagram Reel Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Direct Resumable Video Upload to Meta Graph API for Instagram Reels
    public String createReelViaDirectUpload(Deal deal, File videoFile) {
        try {
            String url = "https://graph.facebook.com/v23.0/" + instagramConfig.getBusinessId() + "/media";

            // Step 1: Initialize Resumable Upload Container
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("media_type", "REELS");
            body.put("upload_type", "resumable");
            body.put("caption", captionService.createCaption(deal));
            body.put("access_token", instagramConfig.getAccessToken());

            HttpHeaders initHeaders = new HttpHeaders();
            initHeaders.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> initRequest = new HttpEntity<>(body, initHeaders);
            ResponseEntity<Map> initResponse = restTemplate.postForEntity(url, initRequest, Map.class);

            if (initResponse.getBody() == null || initResponse.getBody().get("id") == null) {
                System.out.println("❌ Failed to initiate Instagram Reel upload session");
                return null;
            }

            String creationId = initResponse.getBody().get("id").toString();
            String uploadUri = initResponse.getBody().get("uri") != null 
                    ? initResponse.getBody().get("uri").toString() 
                    : "https://rupload.facebook.com/ig-video-upload/" + creationId;

            System.out.println("Initiated Resumable Reel Upload Session [Creation ID: " + creationId + "]");

            // Step 2: Stream video bytes directly to Meta rupload endpoint
            byte[] videoBytes = java.nio.file.Files.readAllBytes(videoFile.toPath());

            HttpHeaders uploadHeaders = new HttpHeaders();
            uploadHeaders.set("Authorization", "OAuth " + instagramConfig.getAccessToken());
            uploadHeaders.set("offset", "0");
            uploadHeaders.set("file_size", String.valueOf(videoBytes.length));
            uploadHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            HttpEntity<byte[]> uploadRequest = new HttpEntity<>(videoBytes, uploadHeaders);
            ResponseEntity<Map> uploadResponse = restTemplate.postForEntity(uploadUri, uploadRequest, Map.class);

            System.out.println("Direct Video Bytes Uploaded to Meta: Status " + uploadResponse.getStatusCode());
            return creationId;

        } catch (Exception e) {
            System.out.println("⚠️ Direct Resumable Reel Upload Warning: " + e.getMessage());
            return null;
        }
    }

    // Create Instagram Image media container
    private String createMediaContainer(Deal deal, String imageUrl) {
        String url = "https://graph.facebook.com/v23.0/" + instagramConfig.getBusinessId() + "/media";

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("image_url", imageUrl);
        body.put("caption", captionService.createCaption(deal));
        body.put("access_token", instagramConfig.getAccessToken());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || responseBody.get("id") == null) {
                return null;
            }

            return responseBody.get("id").toString();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("Instagram API HTTP Error (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
            return null;
        }
    }

    // Create Instagram Reel (Video) media container via URL
    private String createReelMediaContainer(Deal deal, String videoUrl) {
        String url = "https://graph.facebook.com/v23.0/" + instagramConfig.getBusinessId() + "/media";

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("media_type", "REELS");
        body.put("video_url", videoUrl);
        body.put("caption", captionService.createCaption(deal));
        body.put("access_token", instagramConfig.getAccessToken());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || responseBody.get("id") == null) {
                return null;
            }

            return responseBody.get("id").toString();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("Instagram Reel API HTTP Error (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
            return null;
        }
    }

    // Poll status of video container until FINISHED
    private boolean waitForMediaContainerProcessing(String creationId) {
        String statusUrl = "https://graph.facebook.com/v23.0/" + creationId
                + "?fields=status_code,status&access_token=" + instagramConfig.getAccessToken();

        int maxRetries = 20; // 20 * 3 seconds = 60s
        for (int i = 0; i < maxRetries; i++) {
            try {
                Thread.sleep(3000);
                ResponseEntity<Map> response = restTemplate.getForEntity(statusUrl, Map.class);
                if (response.getBody() != null) {
                    String statusCode = (String) response.getBody().get("status_code");
                    System.out.println("Reel status check [" + (i + 1) + "]: " + statusCode);
                    if ("FINISHED".equalsIgnoreCase(statusCode)) {
                        return true;
                    } else if ("ERROR".equalsIgnoreCase(statusCode) || "EXPIRED".equalsIgnoreCase(statusCode)) {
                        System.out.println("❌ Meta Reel Processing Error Details: " + response.getBody());
                        return false;
                    }
                }
            } catch (Exception e) {
                System.out.println("Polling status error: " + e.getMessage());
            }
        }
        return false;
    }

    // Publish Instagram media (Images, Reels & Carousels) with readiness polling & exception handling
    private boolean publishMedia(String creationId) {
        String url = "https://graph.facebook.com/v23.0/" + instagramConfig.getBusinessId() + "/media_publish";

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("creation_id", creationId);
        body.put("access_token", instagramConfig.getAccessToken());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    return true;
                }
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                String errorBody = e.getResponseBodyAsString();
                System.out.println("Instagram Publish API Attempt [" + (i + 1) + "] Status (" + e.getStatusCode() + "): " + errorBody);

                // If error indicates media is still processing (e.g. error code 2207027 "Media ID is not ready to publish")
                if (errorBody.contains("2207027") || errorBody.toLowerCase().contains("not ready") || errorBody.toLowerCase().contains("in_progress")) {
                    System.out.println("⏳ Media container [" + creationId + "] is still processing on Meta servers. Retrying in 3 seconds...");
                    try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                    continue;
                } else {
                    System.out.println("❌ Instagram Publish API Error: " + errorBody);
                    return false;
                }
            } catch (Exception e) {
                System.out.println("❌ Instagram Publish Error: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    /**
     * Publishes a multi-item Carousel post to Instagram using Instagram Graph API.
     */
    public boolean publishInstagramCarousel(java.util.List<Deal> deals, String caption) {
        if (deals == null || deals.isEmpty()) {
            return false;
        }

        try {
            System.out.println("Starting Instagram Carousel upload with " + deals.size() + " items...");

            boolean isLocalServer = serverBaseUrl == null || serverBaseUrl.contains("localhost") || serverBaseUrl.contains("127.0.0.1");
            java.util.List<String> childContainerIds = new java.util.ArrayList<>();
            int slideIndex = 0;
            for (Deal deal : deals) {
                if (childContainerIds.size() >= 10) break; // Instagram Carousel supports max 10 slides
                String imageUrl = deal.getImage();
                if (imageUrl == null || !imageUrl.startsWith("http")) continue;

                // Pre-generate 1:1 post image with title, discount badge, MRP and Offer Price for carousel slide
                String slideUrl = imageUrl;
                try {
                    if (videoGenerationService != null) {
                        videoGenerationService.createPostImage(deal, "generated/carousel_slide_" + slideIndex + ".jpg");
                        if (!isLocalServer) {
                            slideUrl = serverBaseUrl + "/video/carousel-image/" + slideIndex;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Warning: Could not pre-generate carousel slide image: " + e.getMessage());
                }
                slideIndex++;

                // Create child container for carousel item with proxied image URL
                String childId = createCarouselItemContainer(slideUrl);
                if (childId != null) {
                    childContainerIds.add(childId);
                }
            }

            if (childContainerIds.isEmpty()) {
                System.out.println("No valid carousel item containers could be created.");
                return false;
            }

            // Create parent carousel container
            String carouselContainerId = createCarouselParentContainer(childContainerIds, caption);
            if (carouselContainerId == null) {
                System.out.println("Failed to create Instagram parent Carousel container");
                return false;
            }

            System.out.println("Instagram Parent Carousel Container ID: " + carouselContainerId);
            return publishMedia(carouselContainerId);

        } catch (Exception e) {
            System.out.println("Instagram Carousel Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String createCarouselItemContainer(String rawImageUrl) {
        String url = "https://graph.facebook.com/v23.0/" + instagramConfig.getBusinessId() + "/media";
        String targetImageUrl = getProxiedImageUrl(rawImageUrl);

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("image_url", targetImageUrl);
        body.put("is_carousel_item", true);
        body.put("access_token", instagramConfig.getAccessToken());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            return responseBody != null && responseBody.get("id") != null ? responseBody.get("id").toString() : null;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("Error creating carousel item container (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            System.out.println("Error creating carousel item container: " + e.getMessage());
            return null;
        }
    }

    private String createCarouselParentContainer(java.util.List<String> childrenIds, String caption) {
        String url = "https://graph.facebook.com/v23.0/" + instagramConfig.getBusinessId() + "/media";

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("media_type", "CAROUSEL");
        body.put("children", childrenIds);
        if (caption != null && !caption.trim().isEmpty()) {
            body.put("caption", caption);
        }
        body.put("access_token", instagramConfig.getAccessToken());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            return responseBody != null && responseBody.get("id") != null ? responseBody.get("id").toString() : null;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("Error creating parent carousel container (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            System.out.println("Error creating parent carousel container: " + e.getMessage());
            return null;
        }
    }

    /**
     * Publishes a deal to Instagram Story (Image or Video) with affiliate URL link sticker & reliable fallback.
     */
    public boolean publishStory(Deal deal) {
        if (deal == null) return false;
        String affiliateLink = deal.getLink();

        boolean isLocalOrInvalidServer = serverBaseUrl == null 
                || serverBaseUrl.contains("localhost") 
                || serverBaseUrl.contains("127.0.0.1") 
                || serverBaseUrl.contains("a1b2c3d4") 
                || serverBaseUrl.contains("example.com");

        if (!isLocalOrInvalidServer) {
            try {
                if (videoGenerationService != null) {
                    videoGenerationService.createPostImage(deal);
                }
                String formattedImageUrl = serverBaseUrl + "/video/image/stream";
                System.out.println("Attempting Story upload with Price & Offer Price overlay: " + formattedImageUrl);
                boolean success = publishStoryMedia(formattedImageUrl, "IMAGE", affiliateLink);
                if (success) return true;
            } catch (Exception e) {
                System.out.println("⚠️ Warning during Story custom image upload: " + e.getMessage());
            }
        }

        // Always fallback to direct product image (proxied through wsrv.nl)
        System.out.println("Attempting Story post with product image fallback: " + deal.getImage());
        String imageUrl = getProxiedImageUrl(deal.getImage());
        return publishStoryMedia(imageUrl, "IMAGE", affiliateLink);
    }

    public boolean publishStoryMedia(String mediaUrl, String mediaType) {
        return publishStoryMedia(mediaUrl, mediaType, null);
    }

    /**
     * Publishes specified media (image or video URL) to Instagram Story via Meta Graph API with optional link sticker.
     */
    public boolean publishStoryMedia(String mediaUrl, String mediaType, String linkUrl) {
        System.out.println("Starting Instagram Story upload for media: " + mediaUrl);
        try {
            String url = "https://graph.facebook.com/v23.0/" + instagramConfig.getBusinessId() + "/media";

            Map<String, Object> body = new java.util.HashMap<>();
            body.put("media_type", "STORIES");
            if ("VIDEO".equalsIgnoreCase(mediaType)) {
                body.put("video_url", mediaUrl);
            } else {
                body.put("image_url", mediaUrl);
            }
            body.put("access_token", instagramConfig.getAccessToken());

            if (linkUrl != null && !linkUrl.trim().isEmpty()) {
                Map<String, String> sticker = new java.util.HashMap<>();
                sticker.put("url", linkUrl.trim());
                body.put("link_sticker", sticker);
                System.out.println("🔗 Attaching Affiliate Link Sticker to Instagram Story: " + linkUrl);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = null;

            try {
                response = restTemplate.postForEntity(url, request, Map.class);
            } catch (Exception stickerErr) {
                // If link_sticker is not supported for this account type, retry gracefully without it
                if (body.containsKey("link_sticker")) {
                    System.out.println("ℹ️ Account does not support API link_sticker parameter, retrying standard story...");
                    body.remove("link_sticker");
                    request = new HttpEntity<>(body, headers);
                    response = restTemplate.postForEntity(url, request, Map.class);
                } else {
                    throw stickerErr;
                }
            }

            Map<String, Object> responseBody = response != null ? response.getBody() : null;
            if (responseBody == null || responseBody.get("id") == null) {
                System.out.println("❌ Failed to create Instagram Story media container");
                return false;
            }

            String creationId = responseBody.get("id").toString();
            System.out.println("Instagram Story Creation ID: " + creationId);

            if ("VIDEO".equalsIgnoreCase(mediaType)) {
                boolean ready = waitForMediaContainerProcessing(creationId);
                if (!ready) {
                    System.out.println("Story video processing timed out or failed on Meta servers.");
                    return false;
                }
            }

            boolean published = publishMedia(creationId);
            if (published) {
                System.out.println("✅ Instagram Story published successfully!");
            } else {
                System.out.println("❌ Instagram Story publish failed.");
            }
            return published;

        } catch (Exception e) {
            System.out.println("❌ Instagram Story Error: " + e.getMessage());
            return false;
        }
    }
}