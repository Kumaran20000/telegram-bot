package com.example.telegram_bot.config;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;

@Configuration
public class YouTubeConfig {

    private static final String APPLICATION_NAME = "Affiliate YouTube Shorts Bot";
    private static final List<String> YOUTUBE_SCOPES = List.of(
            "https://www.googleapis.com/auth/youtube.upload",
            "https://www.googleapis.com/auth/youtube.force-ssl",
            "https://www.googleapis.com/auth/youtube"
    );

    @Value("${youtube.enabled:true}")
    private boolean youtubeEnabled;

    @Value("${youtube.client.id:${YOUTUBE_CLIENT_ID:}}")
    private String clientId;

    @Value("${youtube.client.secret:${YOUTUBE_CLIENT_SECRET:}}")
    private String clientSecret;

    @Value("${youtube.refresh.token:${YOUTUBE_REFRESH_TOKEN:}}")
    private String refreshToken;

    @Bean
    public YouTube youtubeClient() {
        if (!youtubeEnabled) {
            System.out.println("ℹ️ YouTube client disabled via youtube.enabled=false");
            return null;
        }

        try {
            // 1. Direct OAuth 2.0 Refresh Token (Primary method for YouTube Channel uploads)
            if (clientId != null && !clientId.isBlank() &&
                clientSecret != null && !clientSecret.isBlank() &&
                refreshToken != null && !refreshToken.isBlank()) {

                UserCredentials userCredentials = UserCredentials.newBuilder()
                        .setClientId(clientId.trim())
                        .setClientSecret(clientSecret.trim())
                        .setRefreshToken(refreshToken.trim())
                        .build();

                System.out.println("✅ Loaded YouTube credentials using OAuth 2.0 Refresh Token.");
                return new YouTube.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        new HttpCredentialsAdapter(userCredentials))
                        .setApplicationName(APPLICATION_NAME)
                        .build();
            }

            // 2. Environment Variable GOOGLE_CREDENTIALS_JSON
            String credentialsJson = System.getenv("GOOGLE_CREDENTIALS_JSON");
            if (credentialsJson != null && !credentialsJson.isBlank()) {
                String sanitized = sanitizeCredentialsJson(credentialsJson);
                InputStream stream = new ByteArrayInputStream(sanitized.getBytes(StandardCharsets.UTF_8));
                GoogleCredentials credentials = GoogleCredentials.fromStream(stream).createScoped(YOUTUBE_SCOPES);

                System.out.println("✅ Loaded YouTube credentials from GOOGLE_CREDENTIALS_JSON.");
                return new YouTube.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        new HttpCredentialsAdapter(credentials))
                        .setApplicationName(APPLICATION_NAME)
                        .build();
            }

            // 3. Fallback to local credentials.json in resources
            InputStream stream = getClass().getClassLoader().getResourceAsStream("credentials.json");
            if (stream != null) {
                String rawJson = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                String sanitized = sanitizeCredentialsJson(rawJson);
                GoogleCredentials credentials = GoogleCredentials.fromStream(
                        new ByteArrayInputStream(sanitized.getBytes(StandardCharsets.UTF_8)))
                        .createScoped(YOUTUBE_SCOPES);

                System.out.println("✅ Loaded YouTube credentials from local credentials.json.");
                return new YouTube.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        new HttpCredentialsAdapter(credentials))
                        .setApplicationName(APPLICATION_NAME)
                        .build();
            }

            System.out.println("⚠️ No YouTube credentials provided (YOUTUBE_REFRESH_TOKEN or credentials.json). YouTube auto-uploading will be disabled until configured.");
            return null;

        } catch (Exception e) {
            System.err.println("⚠️ Failed to initialize YouTube client: " + e.getMessage());
            return null;
        }
    }

    private String sanitizeCredentialsJson(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) return rawJson;
        String json = rawJson.trim();

        if (!json.startsWith("{") && json.length() > 50) {
            try {
                byte[] decoded = java.util.Base64.getDecoder().decode(json);
                json = new String(decoded, StandardCharsets.UTF_8);
            } catch (Exception ignored) {}
        }

        if (json.contains("\\n")) {
            json = json.replace("\\n", "\n");
        }

        return json;
    }
}
