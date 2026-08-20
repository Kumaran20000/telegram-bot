package com.example.telegram_bot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a curated Instagram Reels Audio Library track.
 * Strictly filtered for:
 * 1. Showing the Trending (↗️) indicator in Instagram Reels library.
 * 2. Upbeat & Energetic tempo (120-135 BPM).
 * 3. Suitable for e-commerce, product unboxing, dynamic feature reveals, and price drops.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingAudioTrack {
    private String id;
    private String title;                 // e.g. "Makeba (Ian Asher Remix)"
    private String artist;                // e.g. "Jain & Ian Asher"
    private boolean trendingIndicator;    // true (Shows ↗️ in Instagram Reels audio picker)
    private int bpm;                      // e.g. 128 BPM (Upbeat & Energetic)
    private String energyLevel;           // "HIGH_ENERGY", "VERY_HIGH_ENERGY", "PUNCHY_UPBEAT"
    private String mood;                  // e.g. "High-Energy Tech House Drop"
    private String instagramSearchQuery;  // Exact keywords to find in Instagram Reels audio search
    private String productFitDescription; // e.g. "Fast transitions, tech unboxing, dramatic price reveal"
    private List<ProductCategory> suitableCategories;
    private int synthesisPresetIndex;     // Matches high-energy audio synthesis engine (0-5)
}
