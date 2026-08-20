package com.example.telegram_bot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstagramDealItem {
    private int rowNumber;
    private Deal deal;
    private String group;
    private String targetType; // CAROUSEL, REEL, BOTH
    private String status;     // NEW, POSTED, FAILED
    private String dateAdded;
}
