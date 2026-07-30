package com.example.telegram_bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class TelegramService {

    private final RestTemplate restTemplate;

    @Value("${telegram.bot.token}")
    private String token;

    @Value("${telegram.chat.id}")
    private String chatId;

    @Value("${telegram.admin.chat.id:${telegram.chat.id}}")
    private String adminChatId;

    /**
     * Sends administrative notifications and alert messages to the configured admin Telegram chat.
     * Skips sending if adminChatId is blank or identical to the public deal channel to prevent polluting public posts.
     */
    public boolean sendAdminNotification(String alertHtml) {
        try {
            if (adminChatId == null || adminChatId.trim().isEmpty() || adminChatId.trim().equalsIgnoreCase(chatId.trim())) {
                System.out.println("ℹ️ Admin notification suppressed (admin.chat.id is same as public deal channel or blank). Logged alert:\n" + alertHtml);
                return false;
            }
            String url = "https://api.telegram.org/bot" + token + "/sendMessage";

            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("chat_id", adminChatId.trim());
            body.put("text", alertHtml);
            body.put("parse_mode", "HTML");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<java.util.Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, request, String.class);
            System.out.println("🔔 Sent Admin Telegram Notification to " + adminChatId);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send Admin Telegram Notification: " + e.getMessage());
            return false;
        }
    }

    public boolean sendMessage(String message) {

        try {

            String url = "https://api.telegram.org/bot" + token + "/sendMessage";

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("chat_id", chatId);
            body.add("text", message);
            body.add("parse_mode", "Markdown");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request =
                    new HttpEntity<>(body, headers);

            restTemplate.postForObject(url, request, String.class);

            return true;

        } catch (Exception e) {

            System.out.println("Telegram error: " + e.getMessage());

            return false;
        }
    }

    /**
     * Sends a rich photo post with HTML caption and inline action button to Telegram channel.
     */
    public boolean sendPhotoWithButton(String photoUrl, String captionHtml, String buttonText, String buttonUrl) {
        try {
            String url = "https://api.telegram.org/bot" + token + "/sendPhoto";

            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("chat_id", chatId);
            body.put("photo", photoUrl);
            body.put("caption", captionHtml);
            body.put("parse_mode", "HTML");

            if (buttonText != null && buttonUrl != null && !buttonUrl.isEmpty()) {
                java.util.Map<String, Object> button = new java.util.HashMap<>();
                button.put("text", buttonText);
                button.put("url", buttonUrl);

                java.util.Map<String, Object> markup = new java.util.HashMap<>();
                markup.put("inline_keyboard", java.util.List.of(java.util.List.of(button)));
                body.put("reply_markup", markup);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<java.util.Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, request, String.class);
            System.out.println("✅ Telegram Photo post sent successfully with Inline Button!");
            return true;

        } catch (Exception e) {
            System.err.println("Telegram sendPhoto error: " + e.getMessage() + ". Falling back to text message.");
            return sendMessageWithButton(captionHtml, buttonText, buttonUrl);
        }
    }

    /**
     * Sends a rich text message with HTML formatting and inline action button to Telegram channel.
     */
    public boolean sendMessageWithButton(String messageHtml, String buttonText, String buttonUrl) {
        try {
            String url = "https://api.telegram.org/bot" + token + "/sendMessage";

            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", messageHtml);
            body.put("parse_mode", "HTML");
            body.put("disable_web_page_preview", false);

            if (buttonText != null && buttonUrl != null && !buttonUrl.isEmpty()) {
                java.util.Map<String, Object> button = new java.util.HashMap<>();
                button.put("text", buttonText);
                button.put("url", buttonUrl);

                java.util.Map<String, Object> markup = new java.util.HashMap<>();
                markup.put("inline_keyboard", java.util.List.of(java.util.List.of(button)));
                body.put("reply_markup", markup);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<java.util.Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, request, String.class);
            System.out.println("✅ Telegram HTML message sent successfully with Inline Button!");
            return true;

        } catch (Exception e) {
            System.err.println("Telegram sendMessageWithButton error: " + e.getMessage() + ". Falling back to plain text.");
            return sendMessage(messageHtml.replaceAll("<[^>]*>", ""));
        }
    }

    /**
     * Sends a carousel media group (photo album) of up to 10 product deals to Telegram.
     */
    public boolean sendMediaGroup(java.util.List<com.example.telegram_bot.model.Deal> deals, String caption) {
        try {
            String url = "https://api.telegram.org/bot" + token + "/sendMediaGroup";

            java.util.List<java.util.Map<String, Object>> mediaList = new java.util.ArrayList<>();
            for (int i = 0; i < deals.size() && i < 10; i++) {
                com.example.telegram_bot.model.Deal deal = deals.get(i);
                if (deal.getImage() == null || !deal.getImage().startsWith("http")) continue;

                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("type", "photo");
                item.put("media", deal.getImage());
                if (i == 0 && caption != null) {
                    item.put("caption", caption);
                    item.put("parse_mode", "HTML");
                }
                mediaList.add(item);
            }

            if (mediaList.isEmpty()) {
                return false;
            }

            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("chat_id", chatId);
            body.put("media", mediaList);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<java.util.Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, request, String.class);
            System.out.println("Successfully sent Telegram Carousel Media Group (" + mediaList.size() + " items)");
            return true;

        } catch (Exception e) {
            System.out.println("Telegram MediaGroup Error: " + e.getMessage());
            return false;
        }
    }
}