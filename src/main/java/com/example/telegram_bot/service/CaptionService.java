package com.example.telegram_bot.service;

import org.springframework.stereotype.Service;

import com.example.telegram_bot.model.Deal;
import com.example.telegram_bot.model.ProductCategory;

@Service
public class CaptionService {

    private final MessageFormatterService messageFormatterService;

    public CaptionService(MessageFormatterService messageFormatterService) {
        this.messageFormatterService = messageFormatterService;
    }

    public String createCaption(Deal deal) {
        return messageFormatterService.formatInstagramCaption(deal);
    }
}