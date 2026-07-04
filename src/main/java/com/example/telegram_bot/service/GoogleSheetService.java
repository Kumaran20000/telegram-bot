package com.example.telegram_bot.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoogleSheetService {

    private final Sheets sheetsService;
    private final TelegramService telegramService;

    @Value("${google.sheet.id}")
    private String spreadsheetId;

    @Value("${google.sheet.range}")
    private String range;

    public GoogleSheetService(Sheets sheetsService,
                              TelegramService telegramService) {
        this.sheetsService = sheetsService;
        this.telegramService = telegramService;
    }

    public void processSheet() throws Exception {

        ValueRange response = sheetsService.spreadsheets()
                .values()
                .get(spreadsheetId, range)
                .execute();

        List<List<Object>> rows = response.getValues();

        if (rows == null || rows.isEmpty()) {
            System.out.println("No data found");
            return;
        }

        int rowIndex = 2;

        for (List<Object> row : rows) {

            String title = row.size() > 1 ? row.get(1).toString() : "";
            String price = row.size() > 2 ? row.get(2).toString() : "";
            String link = row.size() > 4 ? row.get(4).toString() : "";
            String status = row.size() > 5 ? row.get(5).toString() : "";

            if ("NEW".equalsIgnoreCase(status)) {

                // 1. Build message
                String message =
                        "🔥 HOT DEAL ALERT\n\n" +
                        "🛒 " + title + "\n" +
                        "💰 Price: ₹" + price + "\n\n" +
                        "🔗 Buy Now: " + link;

                // 2. Send to Telegram
                telegramService.sendMessage(message);

                // 3. Mark as POSTED
                updateStatus(rowIndex);

                Thread.sleep(2000);
            }

            rowIndex++;
        }
    }

    private void updateStatus(int rowNumber) throws Exception {

        String updateRange = "Sheet1!F" + rowNumber;

        ValueRange body = new ValueRange()
                .setValues(List.of(List.of("POSTED")));

        sheetsService.spreadsheets().values()
                .update(spreadsheetId, updateRange, body)
                .setValueInputOption("RAW")
                .execute();
    }
}