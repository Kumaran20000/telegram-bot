package com.example.telegram_bot.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.telegram_bot.model.Deal;
import com.example.telegram_bot.model.ProductCategory;
import com.example.telegram_bot.model.TrendingAudioTrack;

public class TrendingInstagramAudioServiceTest {

    private TrendingInstagramAudioService audioService;

    @BeforeEach
    public void setUp() {
        audioService = new TrendingInstagramAudioService();
        audioService.initAudioLibrary();
    }

    @Test
    public void testAllTracksHaveTrendingIndicator() {
        List<TrendingAudioTrack> tracks = audioService.getAllTracks();
        assertNotNull(tracks);
        assertFalse(tracks.isEmpty(), "Audio library should have curated tracks");

        for (TrendingAudioTrack track : tracks) {
            assertTrue(track.isTrendingIndicator(),
                    "Track '" + track.getTitle() + "' MUST show the ↗️ trending indicator in Instagram Reels library");
        }
    }

    @Test
    public void testAllTracksAreUpbeatAndEnergetic() {
        List<TrendingAudioTrack> tracks = audioService.getAllTracks();

        for (TrendingAudioTrack track : tracks) {
            assertTrue(track.getBpm() >= 120 && track.getBpm() <= 135,
                    "Track '" + track.getTitle() + "' must be upbeat & energetic (120-135 BPM), but got: " + track.getBpm());
            assertNotNull(track.getEnergyLevel());
            assertTrue(track.getEnergyLevel().contains("HIGH_ENERGY") || track.getEnergyLevel().contains("UPBEAT"));
        }
    }

    @Test
    public void testAllTracksHaveInstagramSearchQueryAndProductFit() {
        List<TrendingAudioTrack> tracks = audioService.getAllTracks();

        for (TrendingAudioTrack track : tracks) {
            assertNotNull(track.getInstagramSearchQuery(), "Instagram search query must not be null");
            assertFalse(track.getInstagramSearchQuery().trim().isEmpty(), "Instagram search query must not be empty");
            assertNotNull(track.getProductFitDescription(), "Product fit description must not be null");
            assertFalse(track.getProductFitDescription().trim().isEmpty(), "Product fit description must not be empty");
        }
    }

    @Test
    public void testConsecutiveRecommendationsRotateDifferentSongs() {
        Deal deal = new Deal();
        deal.setTitle("boAt Airdopes 141 ANC TWS Earbuds");
        deal.setPrice("1299");

        Set<String> songTitles = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            TrendingAudioTrack track = audioService.getRecommendedTrack(deal, ProductCategory.HEADPHONE);
            assertNotNull(track);
            songTitles.add(track.getTitle());
        }

        // Verify rotation produced multiple different songs
        assertTrue(songTitles.size() > 1,
                "Consecutive recommendations must rotate different songs, got: " + songTitles);
    }

    @Test
    public void testCategoryFiltering() {
        List<TrendingAudioTrack> watchTracks = audioService.getTracksForCategory(ProductCategory.WATCH);
        assertNotNull(watchTracks);
        assertFalse(watchTracks.isEmpty());
        assertTrue(watchTracks.stream().allMatch(t -> t.getSuitableCategories().contains(ProductCategory.WATCH)));

        List<TrendingAudioTrack> kitchenTracks = audioService.getTracksForCategory(ProductCategory.KITCHEN);
        assertNotNull(kitchenTracks);
        assertFalse(kitchenTracks.isEmpty());
        assertTrue(kitchenTracks.stream().allMatch(t -> t.getSuitableCategories().contains(ProductCategory.KITCHEN)));
    }

    @Test
    public void testAudioAdviceFormatting() {
        TrendingAudioTrack track = audioService.getAllTracks().get(0);
        String advice = audioService.formatAudioAdvice(track);
        assertNotNull(advice);
        assertTrue(advice.contains("Reels Audio"));
        assertTrue(advice.contains("↗️"));
        assertTrue(advice.contains(String.valueOf(track.getBpm())));
    }
}
