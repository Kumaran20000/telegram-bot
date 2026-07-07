package com.example.telegram_bot.service;

import org.springframework.stereotype.Service;

import com.example.telegram_bot.model.Deal;
import com.example.telegram_bot.model.ProductCategory;

@Service
public class CaptionService {

    private final CategoryService categoryService;
    private final HashtagService hashtagService;

    public CaptionService(CategoryService categoryService,
                          HashtagService hashtagService) {

        this.categoryService = categoryService;
        this.hashtagService = hashtagService;
    }

    public String createCaption(Deal deal) {

        ProductCategory category =
                categoryService.detectCategory(deal.getTitle());

        String hashtags =
                hashtagService.getHashTags(category);

        return "🔥 LIMITED TIME DEAL 🔥\n\n"
                + "🛍️ " + deal.getTitle() + "\n\n"
                + "💰 Only ₹" + deal.getPrice() + "\n\n"
                + "👇 Comment \"LINK\" and we'll reply with the product link.\n\n"
                + "❤️ Follow @offerzone2538 for daily deals.\n\n"
                + hashtags;
    }

}