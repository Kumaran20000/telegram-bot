package com.example.telegram_bot.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.telegram_bot.model.Deal;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

@Service
public class GoogleSheetService {

    private final Sheets sheetsService;
    private final TelegramService telegramService;
    private final InstagramService instagramService;
    private final VideoGenerationService videoGenerationService;
    private final MessageFormatterService messageFormatterService;

    @Value("${google.sheet.id}")
    private String spreadsheetId;

    @Value("${google.sheet.range}")
    private String range;

    @Value("${app.server.base-url:http://localhost:8080}")
    private String serverBaseUrl;

    public GoogleSheetService(
            Sheets sheetsService,
            TelegramService telegramService,
            InstagramService instagramService,
            VideoGenerationService videoGenerationService,
            MessageFormatterService messageFormatterService) {

        this.sheetsService = sheetsService;
        this.telegramService = telegramService;
        this.instagramService = instagramService;
        this.videoGenerationService = videoGenerationService;
        this.messageFormatterService = messageFormatterService;
    }

    // Save deal into Google Sheet (Prevents duplicate entries)
    public void saveDeal(Deal deal) {

        try {
            if (isDuplicateDeal(deal)) {
                System.out.println("⚠️ Skipping duplicate deal: " + (deal != null ? deal.getTitle() : ""));
                return;
            }

            ValueRange appendBody = new ValueRange()
                    .setValues(Arrays.asList(
                            Arrays.asList(
                                    deal.getTitle(),
                                    deal.getPrice(),
                                    deal.getImage(),
                                    deal.getLink(),
                                    deal.getSource(),
                                    "NEW",
                                    "NEW"
                            )
                    ));

            sheetsService.spreadsheets()
                    .values()
                    .append(spreadsheetId, range, appendBody)
                    .setValueInputOption("RAW")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute();

            System.out.println("Saved deal : " + deal.getTitle());

        } catch (Exception e) {

            System.out.println("Save Deal Error : " + e.getMessage());
        }
    }

    // Check if a deal already exists in Google Sheet (by ASIN, Link, or Title)
    public boolean isDuplicateDeal(Deal deal) {
        if (deal == null) return false;
        try {
            List<List<Object>> rows = getAllRows();
            if (rows == null || rows.isEmpty()) {
                return false;
            }

            String newLink = deal.getLink() != null ? deal.getLink().trim() : "";
            String newTitle = deal.getTitle() != null ? deal.getTitle().trim() : "";
            String newAsin = extractAsin(newLink);

            for (List<Object> row : rows) {
                String existingTitle = row.size() > 0 ? row.get(0).toString().trim() : "";
                String existingLink = row.size() > 3 ? row.get(3).toString().trim() : "";
                String existingAsin = extractAsin(existingLink);

                // 1. Check exact link match
                if (!newLink.isEmpty() && !existingLink.isEmpty() && newLink.equalsIgnoreCase(existingLink)) {
                    return true;
                }

                // 2. Check ASIN match
                if (!newAsin.isEmpty() && newAsin.equalsIgnoreCase(existingAsin)) {
                    return true;
                }

                // 3. Check Title match (ignoring default fallback titles)
                if (!newTitle.isEmpty() && !existingTitle.isEmpty() 
                        && !newTitle.equalsIgnoreCase("Amazon Deal") 
                        && newTitle.equalsIgnoreCase(existingTitle)) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("Duplicate Check Error: " + e.getMessage());
        }
        return false;
    }

    private String extractAsin(String url) {
        if (url == null) return "";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("/(?:dp|gp/product)/([A-Z0-9]{10})").matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    // Update product details for a specific row in Google Sheet
    public void updateDealRow(int rowNumber, Deal deal) {
        try {
            String updateRange = "Sheet1!A" + rowNumber + ":E" + rowNumber;
            ValueRange body = new ValueRange()
                    .setValues(List.of(
                            Arrays.asList(
                                    deal.getTitle() != null ? deal.getTitle() : "",
                                    deal.getPrice() != null ? deal.getPrice() : "",
                                    deal.getImage() != null ? deal.getImage() : "",
                                    deal.getLink() != null ? deal.getLink() : "",
                                    deal.getSource() != null ? deal.getSource() : "Amazon"
                            )
                    ));

            sheetsService.spreadsheets()
                    .values()
                    .update(spreadsheetId, updateRange, body)
                    .setValueInputOption("RAW")
                    .execute();

            System.out.println("Updated Google Sheet Row " + rowNumber + " with deal: " + deal.getTitle());
        } catch (Exception e) {
            System.out.println("Update Deal Row Error on row " + rowNumber + ": " + e.getMessage());
        }
    }

    // Get all rows from Google Sheet
    public List<List<Object>> getAllRows() throws Exception {
        ValueRange response = sheetsService.spreadsheets()
                .values()
                .get(spreadsheetId, range)
                .execute();
        return response.getValues();
    }

    // Process ONLY ONE NEW deal
    public void processNextDeal() throws Exception {

        ValueRange response = sheetsService.spreadsheets()
                .values()
                .get(spreadsheetId, range)
                .execute();

        List<List<Object>> rows = response.getValues();

        if (rows == null || rows.isEmpty()) {
            System.out.println("No data found.");
            return;
        }

        int rowIndex = 2;
        boolean found = false;

        for (List<Object> row : rows) {

            String title = row.size() > 0 ? row.get(0).toString() : "";
            String price = row.size() > 1 ? row.get(1).toString() : "";
            String image = row.size() > 2 ? row.get(2).toString() : "";
            String link = row.size() > 3 ? row.get(3).toString() : "";
            String source = row.size() > 4 ? row.get(4).toString() : "";
            String telegramStatus = row.size() > 5 ? row.get(5).toString() : "";
            String instagramStatus = row.size() > 6 ? row.get(6).toString() : "";

            boolean instagramPending = "NEW".equalsIgnoreCase(instagramStatus) || instagramStatus.trim().isEmpty();
            boolean telegramPending = "NEW".equalsIgnoreCase(telegramStatus) || telegramStatus.trim().isEmpty();
            System.out.println("Row Number        : " + rowIndex);
            System.out.println("Telegram Status   : [" + telegramStatus + "]");
            System.out.println("Instagram Status  : [" + instagramStatus + "]");
            System.out.println("Telegram Pending  : " + telegramPending);
            System.out.println("Instagram Pending : " + instagramPending);
            System.out.println("Row Size = " + row.size());
            System.out.println("Row = " + row);

            if (!telegramPending && !instagramPending) {
                rowIndex++;
                continue;
            }

            // Skip row if essential deal data is missing (title, price, image, or link)
            boolean isTitleMissing = title.trim().isEmpty() || title.trim().equalsIgnoreCase("N/A") || title.trim().equalsIgnoreCase("Amazon Deal");
            boolean isPriceMissing = price.trim().isEmpty() || price.trim().equalsIgnoreCase("N/A");
            boolean isImageMissing = image.trim().isEmpty() || image.trim().equalsIgnoreCase("N/A");
            boolean isLinkMissing = link.trim().isEmpty() || link.trim().equalsIgnoreCase("N/A");

            if (isTitleMissing || isPriceMissing || isImageMissing || isLinkMissing) {
                StringBuilder missing = new StringBuilder();
                if (isTitleMissing) missing.append("Title ");
                if (isPriceMissing) missing.append("Price ");
                if (isImageMissing) missing.append("Image ");
                if (isLinkMissing) missing.append("Link ");

                System.out.println("⚠️ Skipping Row " + rowIndex + " because deal data is incomplete (Missing: " + missing.toString().trim() + ")");
                rowIndex++;
                continue;
            }

            found = true;

            Deal deal = new Deal();
            deal.setTitle(title);
            deal.setPrice(price);
            deal.setImage(image);
            deal.setLink(link);
            deal.setSource(source);

            System.out.println("------------------------------------");
            System.out.println("Posting Deal");
            System.out.println("Title : " + title);
            System.out.println("------------------------------------");

            if (telegramPending) {
                String telegramHtml = messageFormatterService.formatTelegramMessage(deal);
                String storeName = (deal.getSource() != null && !deal.getSource().trim().isEmpty()) ? deal.getSource() : "Amazon";
                String buttonText = "🛒 Buy Now on " + storeName;
                boolean telegramPosted = false;

                if (deal.getImage() != null && deal.getImage().startsWith("http")) {
                    telegramPosted = telegramService.sendPhotoWithButton(
                            deal.getImage(),
                            telegramHtml,
                            buttonText,
                            deal.getLink()
                    );
                } else {
                    telegramPosted = telegramService.sendMessageWithButton(
                            telegramHtml,
                            buttonText,
                            deal.getLink()
                    );
                }

                if (telegramPosted) {
                    updateTelegramStatus(rowIndex, "POSTED");
                } else {
                    updateTelegramStatus(rowIndex, "FAILED");
                    telegramService.sendAdminNotification(
                            "⚠️ <b>Posting Alert (Telegram)</b>\n\n" +
                            "Failed to post deal at Row <b>" + rowIndex + "</b>:\n" +
                            "🛒 <i>" + deal.getTitle() + "</i>"
                    );
                }
            }

            if (instagramPending) {
                System.out.println(">>>>>>>> PROCESSING INSTAGRAM POST / REEL <<<<<<<<");
                boolean instagramPosted = false;

                boolean isLocalServer = serverBaseUrl.contains("localhost") || serverBaseUrl.contains("127.0.0.1");

                if (isLocalServer) {
                    System.out.println("⚠️ WARNING: 'app.server.base-url' is set to localhost (" + serverBaseUrl + ").");
                    System.out.println("⚠️ Meta/Instagram Graph API CANNOT access localhost URLs!");
                    System.out.println("⚠️ To publish Reels or local images, run 'ngrok http 8080' and set 'app.server.base-url=https://<your-ngrok-url>' in application.properties.");
                } else {
                    // Step 1: Try publishing Reel video via public URL
                    String videoUrl = serverBaseUrl + "/video/stream";
                    System.out.println("Attempting Instagram Reel post via Video URL: " + videoUrl);
                    instagramPosted = instagramService.publishReel(deal, videoUrl);

                    // Step 2: Fallback to 1:1 formatted static image via public URL
                    if (!instagramPosted) {
                        String formattedImageUrl = serverBaseUrl + "/video/image/stream";
                        System.out.println("Reel post skipped/failed. Attempting 1:1 formatted image post via: " + formattedImageUrl);
                        instagramPosted = instagramService.publish(deal, formattedImageUrl);
                    }
                }

                // Step 3: Direct fallback to raw public deal image URL if local server or previous attempts failed
                if (!instagramPosted && deal.getImage() != null && deal.getImage().startsWith("http")) {
                    System.out.println("Attempting fallback to direct public deal image URL: " + deal.getImage());
                    instagramPosted = instagramService.publish(deal, deal.getImage());
                }

                if (instagramPosted) {
                    updateInstagramStatus(rowIndex, "POSTED");
                } else {
                    updateInstagramStatus(rowIndex, "FAILED");
                    telegramService.sendAdminNotification(
                            "⚠️ <b>Posting Alert (Instagram)</b>\n\n" +
                            "Failed to publish deal to Instagram at Row <b>" + rowIndex + "</b>:\n" +
                            "🛒 <i>" + deal.getTitle() + "</i>"
                    );
                }
            }

            break; // Process only one deal
        }

        if (!found) {
            System.out.println("No NEW deals available.");
        }
    }

    private void updateInstagramStatus(int rowNumber, String status) throws Exception {

        String updateRange = "Sheet1!G" + rowNumber;

        ValueRange body = new ValueRange()
                .setValues(List.of(List.of(status)));

        sheetsService.spreadsheets()
                .values()
                .update(spreadsheetId, updateRange, body)
                .setValueInputOption("RAW")
                .execute();

        System.out.println("Instagram Status -> " + status);
    }

    // Update Status
    private void updateTelegramStatus(int rowNumber, String status) throws Exception {

        String updateRange = "Sheet1!F" + rowNumber;

        ValueRange body = new ValueRange()
                .setValues(List.of(List.of(status)));

        sheetsService.spreadsheets()
                .values()
                .update(spreadsheetId, updateRange, body)
                .setValueInputOption("RAW")
                .execute();

        System.out.println("Telegram Status -> " + status);
    }
}