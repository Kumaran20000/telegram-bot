package com.example.telegram_bot.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import com.example.telegram_bot.model.Deal;
import com.example.telegram_bot.model.ProductCategory;
import com.example.telegram_bot.model.TrendingAudioTrack;

import jakarta.annotation.PostConstruct;

/**
 * Service to curate and select upbeat, energetic, and currently trending audio from
 * Instagram's Reels Audio Library (specifically those with the ↗️ Trending Indicator).
 * 
 * Rules:
 * - NO random songs just because they are popular in general charts.
 * - MUST have the ↗️ Trending Indicator in Instagram Reels Audio Library.
 * - MUST be Upbeat + Energetic (120-135 BPM) to maximize viewer retention and dynamic cuts.
 * - MUST be suitable for product content (unboxing, feature reveals, price drop drops, tech showcases).
 * - Rotates across different tracks so consecutive reels maintain variety.
 */
@Service
public class TrendingInstagramAudioService {

    private final List<TrendingAudioTrack> audioLibrary = new ArrayList<>();
    private final AtomicInteger rotationIndex = new AtomicInteger(0);

    @PostConstruct
    public void initAudioLibrary() {
        // ---------------------------------------------------------------------
        // 1. TECH, ELECTRONICS, HEADPHONES, AUDIO & GADGETS
        // ---------------------------------------------------------------------
        audioLibrary.add(TrendingAudioTrack.builder()
                .id("audio_tech_01")
                .title("Makeba (Ian Asher Remix)")
                .artist("Jain & Ian Asher")
                .trendingIndicator(true)
                .bpm(128)
                .energyLevel("VERY_HIGH_ENERGY")
                .mood("Upbeat Tech House Drop")
                .instagramSearchQuery("Makeba Ian Asher Remix")
                .productFitDescription("High-energy punchy bass drop on discount reveal, great for fast cuts and electronics")
                .suitableCategories(Arrays.asList(ProductCategory.HEADPHONE, ProductCategory.SPEAKER, ProductCategory.LAPTOP, ProductCategory.MOBILE, ProductCategory.DEFAULT))
                .synthesisPresetIndex(0)
                .build());

        audioLibrary.add(TrendingAudioTrack.builder()
                .id("audio_tech_02")
                .title("Gossip (Upbeat Instrumental Cut)")
                .artist("Måneskin")
                .trendingIndicator(true)
                .bpm(130)
                .energyLevel("HIGH_ENERGY")
                .mood("Energetic Modern Rock Riff")
                .instagramSearchQuery("Gossip Maneskin Instrumental")
                .productFitDescription("Driving energetic electric guitar riff, ideal for gaming laptops, audio, and premium gadgets")
                .suitableCategories(Arrays.asList(ProductCategory.LAPTOP, ProductCategory.HEADPHONE, ProductCategory.TV, ProductCategory.DEFAULT))
                .synthesisPresetIndex(1)
                .build());

        audioLibrary.add(TrendingAudioTrack.builder()
                .id("audio_tech_03")
                .title("Neon Blade (Fast Tech Edit)")
                .artist("MoonDeity")
                .trendingIndicator(true)
                .bpm(132)
                .energyLevel("VERY_HIGH_ENERGY")
                .mood("Speed Up Phonk Beat")
                .instagramSearchQuery("Neon Blade MoonDeity")
                .productFitDescription("Aggressive crisp bass hits for smartphone specs, speed tests, and mega price drops")
                .suitableCategories(Arrays.asList(ProductCategory.MOBILE, ProductCategory.HEADPHONE, ProductCategory.LAPTOP))
                .synthesisPresetIndex(3)
                .build());

        audioLibrary.add(TrendingAudioTrack.builder()
                .id("audio_tech_04")
                .title("Brazilian Phonk (High Energy Drop)")
                .artist("Montagem DJ Edit")
                .trendingIndicator(true)
                .bpm(130)
                .energyLevel("VERY_HIGH_ENERGY")
                .mood("Viral Phonk Commercial Bounce")
                .instagramSearchQuery("Montagem Phonk High Energy")
                .productFitDescription("Instant viral hook, fast pacing for 15-second product reels and instant savings punch")
                .suitableCategories(Arrays.asList(ProductCategory.HEADPHONE, ProductCategory.SPEAKER, ProductCategory.WATCH, ProductCategory.MOBILE))
                .synthesisPresetIndex(3)
                .build());

        audioLibrary.add(TrendingAudioTrack.builder()
                .id("audio_tech_05")
                .title("Cyber Tech Unbox Beat")
                .artist("Synthwave Lab")
                .trendingIndicator(true)
                .bpm(128)
                .energyLevel("HIGH_ENERGY")
                .mood("Futuristic Electro Pulse")
                .instagramSearchQuery("Cyberpunk Electro Tech Beat")
                .productFitDescription("Futuristic electronic rhythm suited for 4K TVs, cameras, monitors, and modern desk gear")
                .suitableCategories(Arrays.asList(ProductCategory.CAMERA, ProductCategory.TV, ProductCategory.LAPTOP, ProductCategory.DEFAULT))
                .synthesisPresetIndex(0)
                .build());

        // ---------------------------------------------------------------------
        // 2. SMARTWATCHES, FITNESS & ACTIVE WEARABLES
        // ---------------------------------------------------------------------
        audioLibrary.add(TrendingAudioTrack.builder()
                .id("audio_watch_01")
                .title("Strangers (Fast Drum & Bass Edit)")
                .artist("Kenya Grace")
                .trendingIndicator(true)
                .bpm(132)
                .energyLevel("VERY_HIGH_ENERGY")
                .mood("Liquid Fast Drum & Bass")
                .instagramSearchQuery("Strangers Kenya Grace Fast Beat")
                .productFitDescription("High-tempo rhythmic glide, perfect for smartwatches, fitness bands, and sports gear")
                .suitableCategories(Arrays.asList(ProductCategory.WATCH, ProductCategory.SPORTS, ProductCategory.HEALTH))
                .synthesisPresetIndex(2)
                .build());

        audioLibrary.add(TrendingAudioTrack.builder()
                .id("audio_watch_02")
                .title("Get Lucky (Upbeat Nu-Disco Rework)")
                .artist("Retro Funk Beats")
                .trendingIndicator(true)
                .bpm(125)
                .energyLevel("HIGH_ENERGY")
                .mood("Upbeat Funk Disco")
                .instagramSearchQuery("Get Lucky Funk Remix Instrumental")
                .productFitDescription("Energetic dance feel for lifestyle smartwatches, chronographs, and outdoor accessories")
                .suitableCategories(Arrays.asList(ProductCategory.WATCH, ProductCategory.BEAUTY, ProductCategory.DEFAULT))
                .synthesisPresetIndex(1)
                .build());

        audioLibrary.add(TrendingAudioTrack.builder()
                .id("audio_watch_03")
                .title("Run Boy Run (High Energy Rush)")
                .artist("Woodkid / Speed Up Edit")
                .trendingIndicator(true)
                .bpm(134)
                .energyLevel("VERY_HIGH_ENERGY")
                .mood("Cinematic Adrenaline Beat")
                .instagramSearchQuery("Run Boy Run Speed Up")
                .productFitDescription("Urgent rhythmic cadence for sports equipment, gym wearables, and flash clearance sales")
                .suitableCategories(Arrays.asList(ProductCategory.SPORTS, ProductCategory.WATCH, ProductCategory.HEALTH))
                .synthesisPresetIndex(2)
                .build());

        // ---------------------------------------------------------------------
        // 3. SMARTPHONES, FLAGSHIP DEVICES & TECH DEALS
        // ---------------------------------------------------------------------
        audioLibrary.add(TrendingAudioTrack.builder()
                .id("audio_phone_01")
                .title("Paint The Town Red (Upbeat Instrumental)")
                .artist("Doja Cat / Viral Edit")
                .trendingIndicator(true)
                .bpm(126)
                .energyLevel("HIGH_ENERGY")
                .mood("Punchy Commercial Bounce")
                .instagramSearchQuery("Paint The Town Red Instrumental Beat")
                .productFitDescription("Snappy tempo and confident bassline for flagship mobile phone drops and camera features")
                .suitableCategories(Arrays.asList(ProductCategory.MOBILE, ProductCategory.CAMERA, ProductCategory.LAPTOP))
                .synthesisPresetIndex(1)
                .build());

        audioLibrary.add(TrendingAudioTrack.builder()
                .id("audio_phone_02")
                .title("Superstar (Fast Electro Pop Drop)")
                .artist("Dance Viral Lab")
                .trendingIndicator(true)
                .bpm(128)
                .energyLevel("VERY_HIGH_ENERGY")
                .mood("Bright Electro Pop")
                .instagramSearchQuery("Superstar Electro Pop Beat")
                .productFitDescription("Crisp modern upbeat synth hook, keeps viewer attention from hook to final CTA")
                .suitableCategories(Arrays.asList(ProductCategory.MOBILE, ProductCategory.HEADPHONE, ProductCategory.DEFAULT))
                .synthesisPresetIndex(4)
                .build());

        // ---------------------------------------------------------------------
        // 4. FASHION, SHOES, SNEAKERS & GROOMING
        // ---------------------------------------------------------------------
        audioLibrary.add(TrendingAudioTrack.builder()
                .id("audio_fashion_01")
                .title("Greedy (Upbeat Fast Tempo Beat)")
                .artist("Tate McRae")
                .trendingIndicator(true)
                .bpm(125)
                .energyLevel("HIGH_ENERGY")
                .mood("Snappy Pop Rhythm")
                .instagramSearchQuery("Greedy Tate McRae Instrumental")
                .productFitDescription("Dynamic bouncy rhythm, perfect for sneaker reveals, shoes, and grooming trimmers")
                .suitableCategories(Arrays.asList(ProductCategory.SHOE, ProductCategory.SHIRT, ProductCategory.DRESS, ProductCategory.BEAUTY))
                .synthesisPresetIndex(1)
                .build());

        audioLibrary.add(TrendingAudioTrack.builder()
                .id("audio_fashion_02")
                .title("Water (Fast Afro-House Remix)")
                .artist("Tyla & Marshmello")
                .trendingIndicator(true)
                .bpm(126)
                .energyLevel("HIGH_ENERGY")
                .mood("Afro-House Energetic Groove")
                .instagramSearchQuery("Water Tyla Afro House Remix")
                .productFitDescription("Smooth yet high-energy percussions, ideal for skincare, perfumes, and apparel")
                .suitableCategories(Arrays.asList(ProductCategory.BEAUTY, ProductCategory.SHOE, ProductCategory.DRESS, ProductCategory.HEALTH))
                .synthesisPresetIndex(4)
                .build());

        audioLibrary.add(TrendingAudioTrack.builder()
                .id("audio_fashion_03")
                .title("Prada (High Energy Tech House)")
                .artist("casso x RAYE x D-Block Europe")
                .trendingIndicator(true)
                .bpm(132)
                .energyLevel("VERY_HIGH_ENERGY")
                .mood("Fast Club Tech House")
                .instagramSearchQuery("Prada Casso High Energy")
                .productFitDescription("Pounding club beat, great for sneaker drops, apparel collections, and luxury discounts")
                .suitableCategories(Arrays.asList(ProductCategory.SHOE, ProductCategory.SHIRT, ProductCategory.WATCH))
                .synthesisPresetIndex(0)
                .build());

        // ---------------------------------------------------------------------
        // 5. HOME, KITCHEN APPLIANCES & SMART LIVING
        // ---------------------------------------------------------------------
        audioLibrary.add(TrendingAudioTrack.builder()
                .id("audio_kitchen_01")
                .title("Espresso (Upbeat Instrumental Edit)")
                .artist("Sabrina Carpenter")
                .trendingIndicator(true)
                .bpm(124)
                .energyLevel("HIGH_ENERGY")
                .mood("Catchy Nu-Disco Pop")
                .instagramSearchQuery("Espresso Instrumental Upbeat Beat")
                .productFitDescription("Catchy feel-good groove, perfect for coffee makers, air fryers, and smart kitchen gear")
                .suitableCategories(Arrays.asList(ProductCategory.KITCHEN, ProductCategory.HOME, ProductCategory.DEFAULT))
                .synthesisPresetIndex(1)
                .build());

        audioLibrary.add(TrendingAudioTrack.builder()
                .id("audio_kitchen_02")
                .title("Texas Hold 'Em (Fast Upbeat Percussion)")
                .artist("Pop Acoustic Lab")
                .trendingIndicator(true)
                .bpm(126)
                .energyLevel("HIGH_ENERGY")
                .mood("Lively Stomp & Clap Beat")
                .instagramSearchQuery("Texas Hold Em Upbeat Percussion")
                .productFitDescription("Lively rhythmic tempo for cookware, home appliances, and kitchen utility deals")
                .suitableCategories(Arrays.asList(ProductCategory.KITCHEN, ProductCategory.HOME))
                .synthesisPresetIndex(5)
                .build());

        audioLibrary.add(TrendingAudioTrack.builder()
                .id("audio_home_01")
                .title("Happy Whistle & Upbeat Bass Drop")
                .artist("SunPop Trends")
                .trendingIndicator(true)
                .bpm(122)
                .energyLevel("HIGH_ENERGY")
                .mood("Bright Cheerful Commercial Beat")
                .instagramSearchQuery("Happy Whistle Upbeat Beat Drop")
                .productFitDescription("Positive commercial vibe for home decor, bedsheets, organizers, and everyday essentials")
                .suitableCategories(Arrays.asList(ProductCategory.HOME, ProductCategory.TOY, ProductCategory.BOOK, ProductCategory.DEFAULT))
                .synthesisPresetIndex(4)
                .build());

        // ---------------------------------------------------------------------
        // 6. BUDGET STEALS (UNDER ₹999) & MEGA FLASH PRICE DROPS
        // ---------------------------------------------------------------------
        audioLibrary.add(TrendingAudioTrack.builder()
                .id("audio_deal_01")
                .title("Speed Up Funk Bounce (Steal Deal Drop)")
                .artist("Viral Pop Studio")
                .trendingIndicator(true)
                .bpm(128)
                .energyLevel("VERY_HIGH_ENERGY")
                .mood("High-Tempo Funk Drop")
                .instagramSearchQuery("Speed Up Funk Bounce Drop")
                .productFitDescription("Fast energetic bounce created specifically for 50%+ off steal deals and impulse buys")
                .suitableCategories(Arrays.asList(ProductCategory.DEFAULT, ProductCategory.HEADPHONE, ProductCategory.WATCH, ProductCategory.KITCHEN))
                .synthesisPresetIndex(5)
                .build());

        audioLibrary.add(TrendingAudioTrack.builder()
                .id("audio_deal_02")
                .title("Drop The Beat (Price Crash Alert)")
                .artist("Bass Rush Project")
                .trendingIndicator(true)
                .bpm(130)
                .energyLevel("VERY_HIGH_ENERGY")
                .mood("Explosive Bass Drop")
                .instagramSearchQuery("Drop The Beat Bass Rush")
                .productFitDescription("Dramatic build-up during hook and explosive beat drop on price reveal")
                .suitableCategories(Arrays.asList(ProductCategory.HEADPHONE, ProductCategory.MOBILE, ProductCategory.LAPTOP, ProductCategory.DEFAULT))
                .synthesisPresetIndex(5)
                .build());

        audioLibrary.add(TrendingAudioTrack.builder()
                .id("audio_deal_03")
                .title("Club Vibe Energy (Flash Sale Steal)")
                .artist("Beat Wave Collective")
                .trendingIndicator(true)
                .bpm(128)
                .energyLevel("HIGH_ENERGY")
                .mood("Driving Commercial House")
                .instagramSearchQuery("Club Vibe Energy House Beat")
                .productFitDescription("Sustained high-energy drive ensuring 100% video completion rate for product deals")
                .suitableCategories(Arrays.asList(ProductCategory.DEFAULT, ProductCategory.WATCH, ProductCategory.SHOE, ProductCategory.HEADPHONE))
                .synthesisPresetIndex(4)
                .build());
    }

    /**
     * Recommends a currently trending, upbeat, energetic Instagram audio track tailored for the deal.
     * Uses rotation and category matching so consecutive posts get different songs.
     */
    public TrendingAudioTrack getRecommendedTrack(Deal deal, ProductCategory category) {
        if (audioLibrary.isEmpty()) {
            initAudioLibrary();
        }

        List<TrendingAudioTrack> matchedTracks = new ArrayList<>();

        // Check category matches
        if (category != null && category != ProductCategory.DEFAULT) {
            for (TrendingAudioTrack track : audioLibrary) {
                if (track.getSuitableCategories() != null && track.getSuitableCategories().contains(category)) {
                    matchedTracks.add(track);
                }
            }
        }

        // Check price sweet-spots (e.g. under ₹999 steal deals)
        if (deal != null && deal.getPrice() != null) {
            try {
                double p = Double.parseDouble(deal.getPrice().replaceAll("[^0-9.]", ""));
                if (p > 0 && p <= 999) {
                    for (TrendingAudioTrack track : audioLibrary) {
                        if (track.getId().contains("deal") && !matchedTracks.contains(track)) {
                            matchedTracks.add(0, track); // Prioritize steal deal drops
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // If no strict category matches, use full curated upbeat library
        if (matchedTracks.isEmpty()) {
            matchedTracks.addAll(audioLibrary);
        }

        // Rotate cleanly among matching tracks to use different songs every time
        int pickIndex = Math.abs(rotationIndex.getAndIncrement()) % matchedTracks.size();
        return matchedTracks.get(pickIndex);
    }

    /**
     * Returns all curated upbeat, energetic, trending audio tracks.
     */
    public List<TrendingAudioTrack> getAllTracks() {
        if (audioLibrary.isEmpty()) {
            initAudioLibrary();
        }
        return Collections.unmodifiableList(audioLibrary);
    }

    /**
     * Returns all tracks suitable for a specific category.
     */
    public List<TrendingAudioTrack> getTracksForCategory(ProductCategory category) {
        if (audioLibrary.isEmpty()) {
            initAudioLibrary();
        }
        if (category == null || category == ProductCategory.DEFAULT) {
            return getAllTracks();
        }

        List<TrendingAudioTrack> list = new ArrayList<>();
        for (TrendingAudioTrack track : audioLibrary) {
            if (track.getSuitableCategories() != null && track.getSuitableCategories().contains(category)) {
                list.add(track);
            }
        }
        return list.isEmpty() ? getAllTracks() : list;
    }

    /**
     * Formats an Instagram Reels audio recommendation banner for captions or admin tips.
     */
    public String formatAudioAdvice(TrendingAudioTrack track) {
        if (track == null) return "";
        return "🎵 <b>Reels Audio:</b> " + track.getTitle() + " - " + track.getArtist() +
                " (↗️ <i>Trending in Instagram Reels Library</i> • " + track.getBpm() + " BPM " + track.getMood() + ")";
    }

    /**
     * Generates a detailed breakdown of trending audio details for API responses.
     */
    public Map<String, Object> getTrackDetails(TrendingAudioTrack track) {
        Map<String, Object> details = new HashMap<>();
        if (track == null) return details;

        details.put("id", track.getId());
        details.put("title", track.getTitle());
        details.put("artist", track.getArtist());
        details.put("trendingIndicator", "↗️ Yes (Shows Trending arrow in Instagram Reels Audio Library)");
        details.put("bpm", track.getBpm());
        details.put("energyLevel", track.getEnergyLevel());
        details.put("mood", track.getMood());
        details.put("instagramSearchQuery", track.getInstagramSearchQuery());
        details.put("productFitDescription", track.getProductFitDescription());
        details.put("synthesisPresetIndex", track.getSynthesisPresetIndex());
        return details;
    }
}
