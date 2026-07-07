package com.example.telegram_bot.service;

import org.springframework.stereotype.Service;

import com.example.telegram_bot.model.ProductCategory;

@Service
public class HashtagService {

    public String getHashTags(ProductCategory category) {

        switch (category) {

            case WATCH:
                return "#watch #watchbox #watchcollector #watchlover "
                        + "#mensfashion #womensfashion #giftideas "
                        + "#amazonfinds #amazondeals #shopping "
                        + "#dealsoftheday #offerzone2538 "
                        + "#discount #sale #india";

            case TV:
                return "#smarttv #androidtv #electronics "
                        + "#homeentertainment #television "
                        + "#amazonfinds #amazondeals "
                        + "#shopping #offerzone2538 "
                        + "#tech #discount";

            case MOBILE:
                return "#smartphone #android #iphone "
                        + "#gadgets #technology "
                        + "#amazonfinds #amazondeals "
                        + "#shopping #offerzone2538 "
                        + "#electronics";

            case LAPTOP:
                return "#laptop #macbook #gaminglaptop "
                        + "#technology #coding "
                        + "#developer #electronics "
                        + "#amazonfinds #amazondeals";

            case SHOE:
                return "#shoes #sneakers #fashion "
                        + "#mensfashion #style "
                        + "#amazonfashion #shopping "
                        + "#offerzone2538";

            case SHIRT:
            case DRESS:
                return "#fashion #style #ootd "
                        + "#amazonfashion #shopping "
                        + "#discount #sale "
                        + "#offerzone2538";

            case KITCHEN:
                return "#kitchen #cookware #homekitchen "
                        + "#cooking #amazonfinds "
                        + "#amazondeals #shopping";

            case BEAUTY:
                return "#beauty #skincare #makeup "
                        + "#amazonbeauty #shopping "
                        + "#offerzone2538";

            default:
                return "#amazonfinds "
                        + "#amazondeals "
                        + "#shopping "
                        + "#shoppingindia "
                        + "#discount "
                        + "#dealsoftheday "
                        + "#offerzone2538";
        }
    }

}