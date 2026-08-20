package com.example.telegram_bot.service;

import com.example.telegram_bot.config.FacebookConfig;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FacebookPageService {

    private final RestTemplate restTemplate;
    private final FacebookConfig facebookConfig;
    private final InstagramConfig instagramConfig;
    private final InstagramService instagramService;
    private final MessageFormatterService messageFormatterService;

    /**
     * Publishes a deal post to a Facebook Page via Meta Graph API.
     */
    public boolean publish(Deal deal) {
        if (!facebookConfig.isEnabled()) {
            System.out.println("Facebook Page posting is disabled in configuration.");
            return false;
        }

        String pageId = facebookConfig.getPageId();
        if (pageId == null || pageId.trim().isEmpty()) {
            System.out.println("⚠️ Facebook Page ID is not configured (facebook.page.id). Skipping Facebook Page post.");
            return false;
        }

        String rawToken = facebookConfig.getEffectivePageAccessToken(instagramConfig.getAccessToken());
        if (rawToken == null || rawToken.trim().isEmpty()) {
            System.out.println("⚠️ Facebook Page Access Token is missing. Skipping Facebook Page post.");
            return false;
        }

        // Auto-resolve specific Page Access Token if input token is a User Access Token
        String pageAccessToken = getPageAccessToken(pageId, rawToken);

        String caption = messageFormatterService.formatFacebookPost(deal);
        String rawImageUrl = deal.getImage();
        boolean success = false;

        if (rawImageUrl != null && rawImageUrl.startsWith("http")) {
            String proxiedImageUrl = instagramService.getProxiedImageUrl(rawImageUrl);
            System.out.println("Starting Facebook Page Photo post to Page ID [" + pageId + "] with URL: " + proxiedImageUrl);
            success = publishPhoto(pageId, pageAccessToken, proxiedImageUrl, caption);
        }

        // Fallback to feed post with link if photo post failed or no image was provided
        if (!success) {
            System.out.println("Attempting Facebook Page Feed post to Page ID [" + pageId + "]");
            success = publishFeedPost(pageId, pageAccessToken, deal.getLink(), caption);
        }

        return success;
    }

    /**
     * Resolves the specific Page Access Token for the target pageId.
     * If the input token is a User Access Token, queries GET /{pageId}?fields=access_token to fetch the Page Access Token.
     */
    public String getPageAccessToken(String pageId, String inputToken) {
        if (inputToken == null || inputToken.trim().isEmpty()) {
            return inputToken;
        }

        try {
            String url = "https://graph.facebook.com/v23.0/" + pageId + "?fields=access_token&access_token=" + inputToken.trim();
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getBody() != null && response.getBody().get("access_token") != null) {
                String pageToken = response.getBody().get("access_token").toString();
                System.out.println("🔑 Automatically resolved Page Access Token for Page ID [" + pageId + "]");
                return pageToken;
            }
        } catch (HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            if (errorBody.contains("190") || errorBody.contains("463") || errorBody.toLowerCase().contains("expired")) {
                System.out.println("🚨 CRITICAL: Facebook/Meta Access Token has EXPIRED! (OAuthException 190, subcode 463). Please update FACEBOOK_PAGE_ACCESS_TOKEN.");
            }
        } catch (Exception e) {
            // Input token may already be a direct Page Access Token
        }
        return inputToken;
    }

    /**
     * Publishes a photo post to a Facebook Page using POST /{page-id}/photos
     */
    public boolean publishPhoto(String pageId, String accessToken, String imageUrl, String caption) {
        String url = "https://graph.facebook.com/v23.0/" + pageId + "/photos";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("url", imageUrl);
        body.add("caption", caption);
        body.add("access_token", accessToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && (responseBody.get("id") != null || responseBody.get("post_id") != null)) {
                String postId = responseBody.get("post_id") != null ? responseBody.get("post_id").toString() : responseBody.get("id").toString();
                System.out.println("✅ Facebook Page photo post published successfully! Post ID: " + postId);
                return true;
            } else {
                System.out.println("❌ Facebook Page photo post failed (No ID returned in response).");
                return false;
            }
        } catch (HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            if (errorBody.contains("190") || errorBody.contains("463") || errorBody.toLowerCase().contains("expired")) {
                System.out.println("🚨 CRITICAL: Facebook Page Access Token EXPIRED! (" + e.getStatusCode() + "): " + errorBody);
            } else if (errorBody.contains("pages_manage_posts") || errorBody.contains("200")) {
                System.out.println("🚨 CRITICAL: Missing 'pages_manage_posts' permission scope! (" + e.getStatusCode() + "): " + errorBody);
            } else {
                System.out.println("❌ Facebook Graph API HTTP Error (" + e.getStatusCode() + "): " + errorBody);
            }
            return false;
        } catch (Exception e) {
            System.out.println("❌ Facebook Page photo post exception: " + e.getMessage());
            return false;
        }
    }

    /**
     * Publishes a text/link post to a Facebook Page using POST /{page-id}/feed
     */
    public boolean publishFeedPost(String pageId, String accessToken, String link, String message) {
        String url = "https://graph.facebook.com/v23.0/" + pageId + "/feed";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("message", message);
        if (link != null && !link.trim().isEmpty()) {
            body.add("link", link);
        }
        body.add("access_token", accessToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.get("id") != null) {
                System.out.println("✅ Facebook Page feed post published successfully! Post ID: " + responseBody.get("id"));
                return true;
            } else {
                System.out.println("❌ Facebook Page feed post failed.");
                return false;
            }
        } catch (HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            if (errorBody.contains("190") || errorBody.contains("463") || errorBody.toLowerCase().contains("expired")) {
                System.out.println("🚨 CRITICAL: Facebook Page Access Token EXPIRED! (" + e.getStatusCode() + "): " + errorBody);
            } else if (errorBody.contains("pages_manage_posts") || errorBody.contains("200")) {
                System.out.println("🚨 CRITICAL: Missing 'pages_manage_posts' permission scope! (" + e.getStatusCode() + "): " + errorBody);
            } else {
                System.out.println("❌ Facebook Graph API HTTP Error (" + e.getStatusCode() + "): " + errorBody);
            }
            return false;
        } catch (Exception e) {
            System.out.println("❌ Facebook Page feed post exception: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks the validity of the configured Facebook Page Access Token against Meta Graph API.
     */
    public Map<String, Object> checkTokenStatus() {
        Map<String, Object> statusMap = new java.util.HashMap<>();
        if (!facebookConfig.isEnabled()) {
            statusMap.put("enabled", false);
            statusMap.put("message", "Facebook Page posting is disabled in configuration.");
            return statusMap;
        }

        String pageId = facebookConfig.getPageId();
        String rawToken = facebookConfig.getEffectivePageAccessToken(instagramConfig.getAccessToken());

        statusMap.put("enabled", true);
        statusMap.put("pageId", pageId);
        statusMap.put("tokenConfigured", rawToken != null && !rawToken.trim().isEmpty());

        if (pageId == null || pageId.trim().isEmpty() || rawToken == null || rawToken.trim().isEmpty()) {
            statusMap.put("valid", false);
            statusMap.put("error", "Missing Facebook Page ID or Access Token configuration.");
            return statusMap;
        }

        try {
            String url = "https://graph.facebook.com/v23.0/" + pageId + "?fields=name,id&access_token=" + rawToken.trim();
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getBody() != null) {
                statusMap.put("valid", true);
                statusMap.put("pageName", response.getBody().get("name"));
                statusMap.put("message", "Facebook Page Access Token is valid and operational!");
            }
        } catch (HttpClientErrorException e) {
            statusMap.put("valid", false);
            statusMap.put("httpStatus", e.getStatusCode().value());
            String errorBody = e.getResponseBodyAsString();
            statusMap.put("errorBody", errorBody);
            if (errorBody.contains("190") || errorBody.contains("463") || errorBody.toLowerCase().contains("expired")) {
                statusMap.put("message", "🚨 Facebook Page Access Token EXPIRED! (OAuthException 190, subcode 463). Please generate a new Page Access Token in Meta Developers console.");
            } else {
                statusMap.put("message", "Meta Graph API Error: " + e.getMessage());
            }
        } catch (Exception e) {
            statusMap.put("valid", false);
            statusMap.put("error", e.getMessage());
        }
        return statusMap;
    }

    /**
     * Publishes a photo story to Facebook Page via Meta Graph API.
     * Uses POST /{page-id}/photo_stories or falls back to photo post.
     */
    public boolean publishStory(Deal deal) {
        if (!facebookConfig.isEnabled() || deal == null) return false;
        String pageId = facebookConfig.getPageId();
        String rawToken = facebookConfig.getEffectivePageAccessToken(instagramConfig.getAccessToken());
        if (pageId == null || pageId.trim().isEmpty() || rawToken == null || rawToken.trim().isEmpty()) {
            System.out.println("⚠️ FB Page configuration missing for Story post.");
            return false;
        }
        String pageAccessToken = getPageAccessToken(pageId, rawToken);
        String proxiedImageUrl = instagramService.getProxiedImageUrl(deal.getImage());

        try {
            // Upload photo as unpublished for story creation
            String uploadUrl = "https://graph.facebook.com/v23.0/" + pageId + "/photos";
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("url", proxiedImageUrl);
            body.add("published", "false");
            body.add("access_token", pageAccessToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            ResponseEntity<Map> response = restTemplate.postForEntity(uploadUrl, new HttpEntity<>(body, headers), Map.class);
            if (response.getBody() != null && response.getBody().get("id") != null) {
                String photoId = response.getBody().get("id").toString();

                // Publish to photo_stories
                String storyUrl = "https://graph.facebook.com/v23.0/" + pageId + "/photo_stories";
                MultiValueMap<String, String> storyBody = new LinkedMultiValueMap<>();
                storyBody.add("photo_id", photoId);
                storyBody.add("access_token", pageAccessToken);

                ResponseEntity<Map> storyResp = restTemplate.postForEntity(storyUrl, new HttpEntity<>(storyBody, headers), Map.class);
                if (storyResp.getBody() != null && (storyResp.getBody().get("id") != null || storyResp.getBody().get("post_id") != null)) {
                    System.out.println("✅ Facebook Page Story published successfully!");
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Facebook Photo Story warning (" + e.getMessage() + "), falling back to Page photo post...");
        }

        // Fallback to photo post
        return publishPhoto(pageId, pageAccessToken, proxiedImageUrl, messageFormatterService.formatFacebookPost(deal));
    }

    /**
     * Publishes a Video Reel to Facebook Page via Meta Graph API.
     */
    public boolean publishReel(Deal deal) {
        return publishReel(deal, null);
    }

    public boolean publishReel(Deal deal, String videoUrl) {
        if (!facebookConfig.isEnabled() || deal == null) return false;
        String pageId = facebookConfig.getPageId();
        String rawToken = facebookConfig.getEffectivePageAccessToken(instagramConfig.getAccessToken());
        if (pageId == null || pageId.trim().isEmpty() || rawToken == null || rawToken.trim().isEmpty()) {
            System.out.println("⚠️ FB Page configuration missing for Reel post.");
            return false;
        }
        String pageAccessToken = getPageAccessToken(pageId, rawToken);
        String caption = messageFormatterService.formatFacebookPost(deal);

        java.io.File videoFile = new java.io.File("generated/reel.mp4");
        String url = "https://graph.facebook.com/v23.0/" + pageId + "/videos";

        // Strategy 1: Direct Multipart File Upload (Works locally without needing public tunnels!)
        if (videoFile.exists() && videoFile.length() > 0) {
            try {
                System.out.println("🚀 Uploading Facebook Reel video directly via Multipart Form Data (" + (videoFile.length() / 1024) + " KB)...");
                MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
                multipartBody.add("source", new org.springframework.core.io.FileSystemResource(videoFile));
                multipartBody.add("description", caption);
                multipartBody.add("access_token", pageAccessToken);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(multipartBody, headers), Map.class);
                if (response.getBody() != null && response.getBody().get("id") != null) {
                    System.out.println("✅ Facebook Page Video Reel published successfully! Video ID: " + response.getBody().get("id"));
                    return true;
                }
            } catch (Exception e) {
                System.out.println("⚠️ Direct Facebook Multipart upload failed: " + e.getMessage() + ", falling back to URL upload...");
            }
        }

        // Strategy 2: URL-based upload
        if (videoUrl != null && !videoUrl.contains("localhost") && !videoUrl.contains("127.0.0.1")) {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("file_url", videoUrl);
            body.add("description", caption);
            body.add("access_token", pageAccessToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
                if (response.getBody() != null && response.getBody().get("id") != null) {
                    System.out.println("✅ Facebook Page Video Reel published successfully! Video ID: " + response.getBody().get("id"));
                    return true;
                }
            } catch (Exception e) {
                System.out.println("❌ Facebook Video Reel Error: " + e.getMessage());
            }
        }

        return false;
    }

    /**
     * Publishes a multi-photo carousel post to Facebook Page using attached_media.
     */
    public boolean publishFacebookCarousel(java.util.List<Deal> deals, String caption) {
        if (!facebookConfig.isEnabled() || deals == null || deals.isEmpty()) return false;
        String pageId = facebookConfig.getPageId();
        String rawToken = facebookConfig.getEffectivePageAccessToken(instagramConfig.getAccessToken());
        if (pageId == null || pageId.trim().isEmpty() || rawToken == null || rawToken.trim().isEmpty()) {
            return false;
        }
        String pageAccessToken = getPageAccessToken(pageId, rawToken);

        java.util.List<String> photoIds = new java.util.ArrayList<>();
        for (Deal deal : deals) {
            if (photoIds.size() >= 10) break;
            String rawImageUrl = deal.getImage();
            if (rawImageUrl == null || !rawImageUrl.startsWith("http")) continue;
            String proxiedUrl = instagramService.getProxiedImageUrl(rawImageUrl);

            try {
                String uploadUrl = "https://graph.facebook.com/v23.0/" + pageId + "/photos";
                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("url", proxiedUrl);
                body.add("published", "false");
                body.add("access_token", pageAccessToken);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                ResponseEntity<Map> response = restTemplate.postForEntity(uploadUrl, new HttpEntity<>(body, headers), Map.class);
                if (response.getBody() != null && response.getBody().get("id") != null) {
                    photoIds.add(response.getBody().get("id").toString());
                }
            } catch (Exception e) {
                System.out.println("Error uploading photo for FB carousel: " + e.getMessage());
            }
        }

        if (photoIds.isEmpty()) return false;

        try {
            String feedUrl = "https://graph.facebook.com/v23.0/" + pageId + "/feed";
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("message", caption);
            body.add("access_token", pageAccessToken);

            for (int i = 0; i < photoIds.size(); i++) {
                body.add("attached_media[" + i + "]", "{\"media_fbid\":\"" + photoIds.get(i) + "\"}");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            ResponseEntity<Map> response = restTemplate.postForEntity(feedUrl, new HttpEntity<>(body, headers), Map.class);
            if (response.getBody() != null && response.getBody().get("id") != null) {
                System.out.println("✅ Facebook Page Carousel Post published successfully! Post ID: " + response.getBody().get("id"));
                return true;
            }
        } catch (Exception e) {
            System.out.println("❌ Facebook Carousel Post Error: " + e.getMessage());
        }
        return false;
    }
}
