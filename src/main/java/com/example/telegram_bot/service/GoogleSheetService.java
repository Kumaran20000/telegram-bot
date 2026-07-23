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

    @Value("${google.sheet.id}")
    private String spreadsheetId;

    @Value("${google.sheet.range}")
    private String range;

    public GoogleSheetService(
            Sheets sheetsService,
            TelegramService telegramService,
            InstagramService instagramService) {

        this.sheetsService = sheetsService;
        this.telegramService = telegramService;
        this.instagramService = instagramService;
    }

    // Save deal into Google Sheet
    public void saveDeal(Deal deal) {

        try {

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

        boolean instagramPending ="NEW".equalsIgnoreCase(instagramStatus) || "FAILED".equalsIgnoreCase(instagramStatus);
        boolean telegramPending ="NEW".equalsIgnoreCase(telegramStatus) || "FAILED".equalsIgnoreCase(telegramStatus);
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

        found = true;

        Deal deal = new Deal();
        deal.setTitle(title);
        deal.setPrice(price);
        deal.setImage(image);
        deal.setLink(link);
        deal.setSource(source);

        String message =
                "🔥 HOT DEAL ALERT\n\n"
                + "🛒 " + title + "\n"
                + "💰 Price: ₹" + price + "\n\n"
                + "🔗 Buy Now:\n"
                + link;

        System.out.println("------------------------------------");
        System.out.println("Posting Deal");
        System.out.println("Title : " + title);
        System.out.println("------------------------------------");

        if (telegramPending) {

            boolean telegramPosted = telegramService.sendMessage(message);

            if (telegramPosted) {
                updateTelegramStatus(rowIndex, "POSTED");
            } else {
                updateTelegramStatus(rowIndex, "FAILED");
            }
        }

        if (instagramPending) {
            System.out.println(">>>>>>>> CALLING INSTAGRAM SERVICE <<<<<<<<");

            boolean instagramPosted = instagramService.publish(deal);

            if (instagramPosted) {
                updateInstagramStatus(rowIndex, "POSTED");
            } else {
                updateInstagramStatus(rowIndex, "FAILED");
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