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