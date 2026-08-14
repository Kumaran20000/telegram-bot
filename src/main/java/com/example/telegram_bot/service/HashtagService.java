package com.example.telegram_bot.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

import com.example.telegram_bot.model.ProductCategory;

@Service
public class HashtagService {

    public String getHashTags(ProductCategory category) {
        return getHashTags(category, "");
    }

    public String getHashTags(ProductCategory category, String title) {
        StringBuilder tags = new StringBuilder();
        String titleLower = title != null ? title.toLowerCase() : "";

        // 1. Extract dynamic Brand Hashtags from title
        List<String> brandTags = extractBrandTags(titleLower);
        for (String bt : brandTags) {
            tags.append("#").append(bt).append(" ");
        }

        // 2. Extract dynamic Product-Specific Keyword Tags from title
        if (titleLower.contains("earbuds") || titleLower.contains("tws") || titleLower.contains("airpods")) {
            tags.append("#earbuds #tws #wireless #audio #bluetooth #noiseisolation ");
        } else if (titleLower.contains("headphone") || titleLower.contains("headset")) {
            tags.append("#headphones #audiophile #basshead #music #wireless ");
        } else if (titleLower.contains("speaker") || titleLower.contains("soundbar")) {
            tags.append("#bluetoothspeaker #soundbar #homeaudio #partybox ");
        } else if (titleLower.contains("watch") || titleLower.contains("smartwatch")) {
            tags.append("#smartwatch #fitness #watchbox #tech #wristwear ");
        } else if (titleLower.contains("laptop") || titleLower.contains("macbook")) {
            tags.append("#laptop #techdeals #gaminglaptop #macbook #productivity ");
        } else if (titleLower.contains("phone") || titleLower.contains("mobile") || titleLower.contains("iphone") || titleLower.contains("samsung")) {
            tags.append("#smartphone #mobiledeals #techtrends #android #iphone ");
        } else if (titleLower.contains("shoe") || titleLower.contains("sneaker")) {
            tags.append("#shoes #sneakers #footwear #sneakerhead #kicks ");
        } else if (titleLower.contains("backpack") || titleLower.contains("bag")) {
            tags.append("#backpack #travelbag #schoolbag #everydaycarry ");
        } else if (titleLower.contains("trimmer") || titleLower.contains("shaver") || titleLower.contains("grooming")) {
            tags.append("#grooming #mensgrooming #trimmer #beardcare ");
        } else if (titleLower.contains("bottle") || titleLower.contains("flask")) {
            tags.append("#waterbottle #thermos #hydration #fitness ");
        } else if (titleLower.contains("fryer") || titleLower.contains("cooker") || titleLower.contains("kettle") || titleLower.contains("kitchen")) {
            tags.append("#kitchengadgets #homeappliances #airfryer #cooking ");
        } else if (titleLower.contains("chair") || titleLower.contains("desk") || titleLower.contains("lamp") || titleLower.contains("home")) {
            tags.append("#homedecor #homefinds #interior #desksetup ");
        }

        // 3. Category Fallback Hashtags
        switch (category) {
            case WATCH:
                tags.append("#smartwatch #watchcollector #watchlover #mensfashion ");
                break;
            case TV:
                tags.append("#smarttv #androidtv #electronics #homeentertainment #4k ");
                break;
            case MOBILE:
                tags.append("#smartphone #mobiledeals #gadgets #technology ");
                break;
            case LAPTOP:
                tags.append("#laptop #gaminglaptop #studentdeals #workfromhome ");
                break;
            case HEADPHONE:
            case SPEAKER:
                tags.append("#earbuds #bluetooth #headphones #music #audio ");
                break;
            case SHOE:
                tags.append("#shoes #sneakers #fashion #sneakerhead #amazonfashion ");
                break;
            case SHIRT:
            case DRESS:
                tags.append("#fashion #style #ootd #outfitinspo #amazonfashion ");
                break;
            case KITCHEN:
            case HOME:
                tags.append("#kitchen #homekitchen #homedecor #appliances #homefinds ");
                break;
            case BEAUTY:
            case HEALTH:
                tags.append("#beauty #skincare #makeup #selfcare #grooming ");
                break;
            default:
                tags.append("#amazongadgets #trendingfinds #usefulproducts ");
                break;
        }

        // 4. Clean Viral Deal Hashtags
        tags.append("#amazonfinds #amazonmusthaves #amazondeals #dealsoftheday #lootdeal #budgetfinds #salealert #offerzone2538");

        return tags.toString().replaceAll("\\s+", " ").trim();
    }

    private List<String> extractBrandTags(String titleLower) {
        List<String> brands = new ArrayList<>();
        String[] knownBrands = {
            "boat", "noise", "boult", "realme", "samsung", "apple", "sony", "jbl", "zebronics", 
            "ptron", "oneplus", "oppo", "vivo", "redmi", "xiaomi", "lenovo", "hp", "dell", 
            "asus", "acer", "logitech", "nike", "puma", "adidas", "fastrack", "pigeon", 
            "prestige", "milton", "solimo", "amazonbasics", "portronics", "crossbeats", "fire-boltt"
        };

        for (String brand : knownBrands) {
            if (titleLower.contains(brand)) {
                String cleanBrand = brand.replace("-", "").replace(" ", "");
                brands.add(cleanBrand);
            }
        }
        return brands;
    }
}