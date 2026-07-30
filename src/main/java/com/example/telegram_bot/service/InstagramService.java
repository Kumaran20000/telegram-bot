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

import java.util.Map;

@Service
@RequiredArgsConstructor
public class InstagramService {

    private final RestTemplate restTemplate;
    private final InstagramConfig instagramConfig;
    private final CaptionService captionService;

    // Main method to publish static image post using deal's image URL
    public boolean publish(Deal deal) {
        return publish(deal, deal.getImage());
    }

    // Overloaded method to publish static image post using custom image URL (e.g. formatted 1:1 image)
    public boolean publish(Deal deal, String imageUrl) {
        System.out.println("Business ID: " + instagramConfig.getBusinessId());
        System.out.println("Access Token: " + instagramConfig.getAccessToken());

        try {
            System.out.println("Starting Instagram Image upload with URL: " + imageUrl);

            if (!isValidInstagramAspectRatio(imageUrl)) {
                System.out.println("❌ Skipping Instagram upload: Image aspect ratio is not supported by Instagram (must be between 0.80 and 1.91).");
                return false;
            }

            String creationId = createMediaContainer(deal, imageUrl);

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

    // Method to publish an Instagram Reel (Video)
    public boolean publishReel(Deal deal, String videoUrl) {
        System.out.println("Starting Instagram Reel upload...");
        try {
            String creationId = createReelMediaContainer(deal, videoUrl);
            if (creationId == null) {
                System.out.println("Failed to create Instagram Reel container");
                return false;
            }

            System.out.println("Reel Creation ID: " + creationId);

            // Wait for video processing on Meta servers
            boolean ready = waitForMediaContainerProcessing(creationId);
            if (!ready) {
                System.out.println("Reel video processing timed out or failed on Meta servers.");
                return false;
            }

            boolean published = publishMedia(creationId);
            if (published) {
                System.out.println("Instagram Reel published successfully");
            } else {
                System.out.println("Instagram Reel publish failed");
            }
            return published;

        } catch (Exception e) {
            System.out.println("Instagram Reel Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Create Instagram Image media container
    private String createMediaContainer(Deal deal, String imageUrl) {
        String url = "https://graph.facebook.com/v23.0/" + instagramConfig.getBusinessId() + "/media";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("image_url", imageUrl);
        body.add("caption", captionService.createCaption(deal));
        body.add("access_token", instagramConfig.getAccessToken());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
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

    // Create Instagram Reel (Video) media container
    private String createReelMediaContainer(Deal deal, String videoUrl) {
        String url = "https://graph.facebook.com/v23.0/" + instagramConfig.getBusinessId() + "/media";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("media_type", "REELS");
        body.add("video_url", videoUrl);
        body.add("caption", captionService.createCaption(deal));
        body.add("access_token", instagramConfig.getAccessToken());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
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

    // Publish Instagram media (Images & Reels)
    private boolean publishMedia(String creationId) {
        String url = "https://graph.facebook.com/v23.0/" + instagramConfig.getBusinessId() + "/media_publish";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("creation_id", creationId);
        body.add("access_token", instagramConfig.getAccessToken());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        return response.getStatusCode().is2xxSuccessful();
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

            java.util.List<String> childContainerIds = new java.util.ArrayList<>();
            for (Deal deal : deals) {
                if (childContainerIds.size() >= 10) break; // Instagram Carousel supports max 10 slides
                String imageUrl = deal.getImage();
                if (imageUrl == null || !imageUrl.startsWith("http")) continue;

                // Create child container for carousel item
                String childId = createCarouselItemContainer(imageUrl);
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

    private String createCarouselItemContainer(String imageUrl) {
        String url = "https://graph.facebook.com/v23.0/" + instagramConfig.getBusinessId() + "/media";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("image_url", imageUrl);
        body.add("is_carousel_item", "true");
        body.add("access_token", instagramConfig.getAccessToken());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            return responseBody != null && responseBody.get("id") != null ? responseBody.get("id").toString() : null;
        } catch (Exception e) {
            System.out.println("Error creating carousel item container: " + e.getMessage());
            return null;
        }
    }

    private String createCarouselParentContainer(java.util.List<String> childrenIds, String caption) {
        String url = "https://graph.facebook.com/v23.0/" + instagramConfig.getBusinessId() + "/media";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("media_type", "CAROUSEL");
        body.add("children", String.join(",", childrenIds));
        if (caption != null && !caption.trim().isEmpty()) {
            body.add("caption", caption);
        }
        body.add("access_token", instagramConfig.getAccessToken());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            return responseBody != null && responseBody.get("id") != null ? responseBody.get("id").toString() : null;
        } catch (Exception e) {
            System.out.println("Error creating parent carousel container: " + e.getMessage());
            return null;
        }
    }
}