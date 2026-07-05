package com.example.telegram_bot.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class GoogleSheetsConfig {

    private static final String APPLICATION_NAME = "Affiliate Bot";

    @Bean
    public Sheets sheetsService() throws Exception {

        GoogleCredentials credentials;

        // 1. Try Railway Environment Variable
        String credentialsJson = System.getenv("GOOGLE_CREDENTIALS_JSON");

        if (credentialsJson != null && !credentialsJson.isBlank()) {

            InputStream stream = new ByteArrayInputStream(
                    credentialsJson.getBytes(StandardCharsets.UTF_8));

            credentials = GoogleCredentials.fromStream(stream)
                    .createScoped(List.of("https://www.googleapis.com/auth/spreadsheets"));

            System.out.println("Loaded Google credentials from Railway Environment Variable.");

        } else {

            // 2. Fallback to local credentials.json
            InputStream stream = getClass()
                    .getClassLoader()
                    .getResourceAsStream("credentials.json");

            if (stream == null) {
                throw new RuntimeException(
                        "Google credentials not found. " +
                        "Set GOOGLE_CREDENTIALS on Railway or add credentials.json locally."
                );
            }

            credentials = GoogleCredentials.fromStream(stream)
                    .createScoped(List.of("https://www.googleapis.com/auth/spreadsheets"));

            System.out.println("Loaded Google credentials from local credentials.json.");
        }

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}