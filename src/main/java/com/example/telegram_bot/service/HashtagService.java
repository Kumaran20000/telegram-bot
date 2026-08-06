package com.example.telegram_bot.service;

import org.springframework.stereotype.Service;

import com.example.telegram_bot.model.ProductCategory;

@Service
public class HashtagService {

    public String getHashTags(ProductCategory category) {

        String commonViralTags = " #amazonfinds #amazonmusthaves #amazondeals #dealsoftheday #lootdeal #trendingdeals #budgetfinds #salealert #shoppingindia #offerzone2538";

        switch (category) {

            case WATCH:
                return "#watch #smartwatch #watchbox #watchcollector #watchlover "
                        + "#mensfashion #womensfashion #giftideas #style"
                        + commonViralTags;

            case TV:
                return "#smarttv #androidtv #electronics "
                        + "#homeentertainment #television #4k #techdeals"
                        + commonViralTags;

            case MOBILE:
                return "#smartphone #android #iphone #mobiledeals "
                        + "#gadgets #technology #techtrends"
                        + commonViralTags;

            case LAPTOP:
                return "#laptop #macbook #gaminglaptop #studentdeals "
                        + "#technology #coding #workfromhome #techdeals"
                        + commonViralTags;

            case HEADPHONE:
            case SPEAKER:
                return "#earbuds #bluetooth #headphones #music #audio "
                        + "#gadgets #tws #sounddeals"
                        + commonViralTags;

            case SHOE:
                return "#shoes #sneakers #fashion #sneakerhead "
                        + "#mensfashion #footwear #style #amazonfashion"
                        + commonViralTags;

            case SHIRT:
            case DRESS:
                return "#fashion #style #ootd #outfitinspo "
                        + "#amazonfashion #shopping #wardrobe #sale"
                        + commonViralTags;

            case KITCHEN:
            case HOME:
                return "#kitchen #cookware #homekitchen #homedecor "
                        + "#cooking #appliances #homefinds"
                        + commonViralTags;

            case BEAUTY:
            case HEALTH:
                return "#beauty #skincare #makeup #selfcare "
                        + "#amazonbeauty #glowing #grooming"
                        + commonViralTags;

            default:
                return "#amazonfinds #amazonmusthaves #amazondeals "
                        + "#shopping #shoppingindia #discount "
                        + "#dealsoftheday #lootdeal #trendingdeals #offerzone2538";
        }
    }

}