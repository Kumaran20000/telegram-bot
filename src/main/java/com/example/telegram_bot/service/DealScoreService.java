package com.example.telegram_bot.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.telegram_bot.model.Deal;
import com.example.telegram_bot.model.ProductCategory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DealScoreService {

    private final CategoryService categoryService;

    /**
     * Calculates and attaches the complete Deal Score and score breakdown to a Deal object.
     * Formula: Deal Score = Discount % + Price Attractiveness + Product Popularity + Category Demand + Previous Performance Score
     */
    public Deal scoreDeal(Deal deal) {
        return scoreDeal(deal, 15.0); // Default 15 pts baseline performance score for active deals
    }

    public Deal scoreDeal(Deal deal, double previousPerformanceScore) {
        if (deal == null) return null;
        ProductCategory category = categoryService.detectCategory(deal.getTitle());
        deal.computeDealScore(category, previousPerformanceScore);
        return deal;
    }

    /**
     * Ranks and sorts a list of deals in descending order of Deal Score (highest score first).
     */
    public List<Deal> rankDeals(List<Deal> deals) {
        if (deals == null || deals.isEmpty()) return deals;

        for (Deal deal : deals) {
            scoreDeal(deal);
        }

        return deals.stream()
                .sorted(Comparator.comparingDouble(Deal::getDealScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Returns the single highest-scoring deal from a list of candidate deals.
     */
    public Deal getTopRankedDeal(List<Deal> deals) {
        List<Deal> ranked = rankDeals(deals);
        return (ranked != null && !ranked.isEmpty()) ? ranked.get(0) : null;
    }
}
