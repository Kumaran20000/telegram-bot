package com.example.telegram_bot.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.telegram_bot.model.ProductCategory;

@Service
public class HashtagService {

    public String getHashTags(ProductCategory category) {
        return getHashTags(category, "", null, null, null);
    }

    public String getHashTags(ProductCategory category, String title) {
        return getHashTags(category, title, null, null, null);
    }

    /**
     * Generates a full, rich set of viral and product-targeted hashtags for Instagram.
     */
    public String getHashTags(ProductCategory category, String title, String price, String mrp, String discount) {
        Set<String> tags = new LinkedHashSet<>();
        String titleLower = title != null ? title.toLowerCase() : "";

        // 1. Dynamic Brand Hashtags (detected from product title)
        List<String> brandTags = extractBrandTags(titleLower);
        for (String bt : brandTags) {
            tags.add("#" + bt);
        }

        // 2. Deep Product-Specific Keyword Hashtags
        extractProductSpecificTags(titleLower, tags);

        // 3. Price & Discount Specific Hashtags
        extractPriceAndDiscountTags(titleLower, price, mrp, discount, tags);

        // 4. Category-Specific Niche & Community Hashtags
        addCategoryFallbackTags(category, tags);

        // 5. Universal Viral Deal Hashtags
        addViralDealTags(tags);

        return String.join(" ", tags).replaceAll("\\s+", " ").trim();
    }

    /**
     * Generates concise, punchy hashtags (3-6 tags) tailored for Telegram posts.
     */
    public String getTelegramHashTags(ProductCategory category, String title) {
        Set<String> tags = new LinkedHashSet<>();
        String titleLower = title != null ? title.toLowerCase() : "";

        // Top Brand Tag
        List<String> brandTags = extractBrandTags(titleLower);
        if (!brandTags.isEmpty()) {
            tags.add("#" + brandTags.get(0));
        }

        // Top Category Tag
        if (category != null && category != ProductCategory.DEFAULT) {
            tags.add("#" + capitalize(category.name().toLowerCase()));
        }

        // Product Keyword Tag
        extractTopProductKeywordTag(titleLower, tags);

        tags.add("#AmazonDeals");
        tags.add("#LootDeal");
        tags.add("#OfferZone");

        return String.join(" ", tags).trim();
    }

    /**
     * Generates clean, balanced hashtags (5-8 tags) tailored for Facebook posts.
     */
    public String getFacebookHashTags(ProductCategory category, String title) {
        Set<String> tags = new LinkedHashSet<>();
        String titleLower = title != null ? title.toLowerCase() : "";

        List<String> brandTags = extractBrandTags(titleLower);
        for (int i = 0; i < Math.min(2, brandTags.size()); i++) {
            tags.add("#" + brandTags.get(i));
        }

        extractTopProductKeywordTag(titleLower, tags);

        if (category != null && category != ProductCategory.DEFAULT) {
            tags.add("#" + capitalize(category.name().toLowerCase()));
        }

        tags.add("#AmazonFinds");
        tags.add("#DealsOfTheDay");
        tags.add("#OnlineShopping");
        tags.add("#OfferZone");

        return String.join(" ", tags).trim();
    }

    /**
     * Generates a structured breakdown of hashtags for a product, useful for REST APIs and diagnostics.
     */
    public Map<String, Object> generateProductHashtagDetails(
            String title, String price, String mrp, String discount, ProductCategory category) {
        Map<String, Object> details = new LinkedHashMap<>();
        String titleLower = title != null ? title.toLowerCase() : "";

        List<String> brandTags = extractBrandTags(titleLower);
        Set<String> productTags = new LinkedHashSet<>();
        extractProductSpecificTags(titleLower, productTags);

        Set<String> priceTags = new LinkedHashSet<>();
        extractPriceAndDiscountTags(titleLower, price, mrp, discount, priceTags);

        Set<String> categoryTags = new LinkedHashSet<>();
        addCategoryFallbackTags(category, categoryTags);

        String instagramTags = getHashTags(category, title, price, mrp, discount);
        String telegramTags = getTelegramHashTags(category, title);
        String facebookTags = getFacebookHashTags(category, title);

        details.put("title", title);
        details.put("category", category != null ? category.name() : "DEFAULT");
        details.put("brandTags", brandTags);
        details.put("productTags", new ArrayList<>(productTags));
        details.put("priceTags", new ArrayList<>(priceTags));
        details.put("categoryTags", new ArrayList<>(categoryTags));
        details.put("instagramHashtags", instagramTags);
        details.put("telegramHashtags", telegramTags);
        details.put("facebookHashtags", facebookTags);
        details.put("totalHashtagsCount", instagramTags.isEmpty() ? 0 : instagramTags.split(" ").length);

        return details;
    }

    /**
     * Extracts brand tags from a comprehensive list of 120+ top consumer brands.
     */
    public List<String> extractBrandTags(String titleLower) {
        List<String> brands = new ArrayList<>();
        if (titleLower == null || titleLower.isEmpty()) {
            return brands;
        }

        String[] knownBrands = {
            // Audio & Electronics
            "boat", "noise", "boult", "realme", "samsung", "apple", "sony", "jbl", "zebronics",
            "ptron", "oneplus", "oppo", "vivo", "redmi", "xiaomi", "lenovo", "hp", "dell",
            "asus", "acer", "logitech", "portronics", "crossbeats", "fire-boltt", "fireboltt",
            "mivi", "truke", "hammer", "ambrane", "syska", "havells", "philips", "bajaj",
            "wipro", "crompton", "orient", "atomberg", "usha", "panasonic", "lg", "toshiba",
            "tcl", "hisense", "iffalcon", "motorola", "moto", "infinix", "poco", "tecno",
            "iqoo", "nothing", "cmf", "honor", "google pixel", "google", "anker", "soundcore",
            "sennheiser", "marshall", "bose", "skullcandy", "cosmic byte", "redgear", "razer",
            "hyperx", "ant esports", "sandisk", "seagate", "western digital", "crucial", "kingston",
            "transcend", "tp-link", "netgear", "d-link", "canon", "nikon", "gopro", "dji", "insta360",

            // Fashion & Footwear
            "nike", "puma", "adidas", "reebok", "under armour", "asics", "skechers", "campus",
            "sparx", "bata", "woodland", "red tape", "crocs", "clarks", "metro", "mochi",
            "fastrack", "fossil", "titan", "casio", "timex", "sonata", "maxima",
            "daniel wellington", "tommy hilfiger", "levis", "levi's", "wrangler", "pepe jeans",
            "spykar", "us polo", "u.s. polo", "allen solly", "peter england", "van heusen",
            "louis philippe", "blackberrys", "arrow", "raymond", "flying machine", "mufti",
            "killer", "jack & jones", "jack and jones", "zara", "h&m", "biba", "w for woman",
            "aurelia", "soch", "fabindia", "manyavar", "lavie", "caprese", "baggit", "lino perros",
            "safari", "american tourister", "skybags", "vip", "wildcraft",

            // Kitchen, Home & Living
            "prestige", "pigeon", "hawkins", "butterfly", "milton", "cello", "borosil",
            "wonderchef", "solimo", "amazonbasics", "agaro", "lifelong", "kent", "aquaguard",
            "eureka forbes", "pureit", "dyson", "morphy richards", "inalsa", "sujata",
            "preethi", "bosch", "ifb", "voltas", "daikin", "godrej", "whirlpool", "haier",
            "wakefit", "sleepwell", "kurlon", "green soul", "featherlite", "cellbell",

            // Beauty, Personal Care & Grooming
            "mamaearth", "wow skin science", "wow", "plum", "dot & key", "dot and key",
            "minimalist", "cetaphil", "the derma co", "derma co", "mcaffeine", "beardo", "ustraa",
            "bombay shaving company", "nivea", "garnier", "loreal", "l'oreal", "maybelline",
            "lakme", "sugar", "nykaa", "biotique", "himalaya", "dove", "tresemme", "biolage",
            "indulekha", "kamasutra", "wild stone", "fogg", "axe", "park avenue", "denver",
            "villain", "bella vita", "engage",

            // Fitness & Health Supplements
            "muscleblaze", "optimum nutrition", "asitis", "myprotein", "isopure", "avvatar",
            "nutrabay", "bigmuscles", "fast&up", "fast and up", "healthkart", "kapiva",
            "oziva", "dr morepen", "omron", "accu-chek", "boldfit", "strauss", "nivia",
            "yonex", "cosco", "decathlon"
        };

        for (String brand : knownBrands) {
            if (titleLower.contains(brand)) {
                String cleanBrand = brand
                        .replace("&", "and")
                        .replace("-", "")
                        .replace("'", "")
                        .replace(".", "")
                        .replace(" ", "");
                if (!brands.contains(cleanBrand)) {
                    brands.add(cleanBrand);
                }
            }
        }
        return brands;
    }

    /**
     * Extracts deep, high-converting product keyword tags from the product title.
     */
    private void extractProductSpecificTags(String titleLower, Set<String> tags) {
        // TWS / Earbuds / AirPods / Neckbands
        if (titleLower.contains("earbuds") || titleLower.contains("tws") || titleLower.contains("airpods")
                || titleLower.contains("airdopes") || titleLower.contains("earpod") || titleLower.contains("neckband")) {
            addTags(tags, "#earbuds", "#tws", "#wirelessearbuds", "#bluetoothaudio", "#noisecancelling", "#anc", "#twsdeals");
        }

        // Headphones & Headsets
        if (titleLower.contains("headphone") || titleLower.contains("headset") || titleLower.contains("over ear")) {
            addTags(tags, "#headphones", "#audiophile", "#basshead", "#wirelessheadphones", "#musiclover", "#studiosound");
        }

        // Speakers & Soundbars
        if (titleLower.contains("speaker") || titleLower.contains("soundbar") || titleLower.contains("partybox") || titleLower.contains("home theatre")) {
            addTags(tags, "#bluetoothspeaker", "#soundbar", "#portablespeaker", "#homeaudio", "#partyspeaker", "#hometheatre");
        }

        // Smartwatch & Fitness Trackers
        if (titleLower.contains("smartwatch") || titleLower.contains("fitness band") || titleLower.contains("smart band") || titleLower.contains("fitness tracker")) {
            addTags(tags, "#smartwatch", "#fitnessband", "#fitnesstracker", "#wristwear", "#smartwatchdeals", "#wearabletech");
        } else if (titleLower.contains("watch") || titleLower.contains("chronograph") || titleLower.contains("analog watch")) {
            addTags(tags, "#watchcollector", "#menswatch", "#luxurywatches", "#chronograph", "#wriststyle", "#analogwatch");
        }

        // Smartphones & Mobiles
        if (titleLower.contains("iphone")) {
            addTags(tags, "#iphone", "#apple", "#ios", "#iphonedeals", "#shotoniphone", "#smartphone");
        } else if (titleLower.contains("samsung galaxy") || titleLower.contains("galaxy")) {
            addTags(tags, "#samsunggalaxy", "#samsungdeals", "#galaxyphone", "#android", "#smartphone");
        } else if (titleLower.contains("phone") || titleLower.contains("mobile") || titleLower.contains("smartphone") || titleLower.contains("5g")) {
            addTags(tags, "#smartphone", "#mobiledeals", "#5gphone", "#techtrends", "#android", "#mobilephotography");
        }

        // Laptops & MacBooks
        if (titleLower.contains("macbook")) {
            addTags(tags, "#macbook", "#apple", "#macbookair", "#macbookpro", "#appledeals", "#laptop");
        } else if (titleLower.contains("gaming laptop") || titleLower.contains("rtx") || titleLower.contains("gtx")) {
            addTags(tags, "#gaminglaptop", "#pcgaming", "#gamingsetup", "#laptopdeals", "#gamerlife");
        } else if (titleLower.contains("laptop") || titleLower.contains("notebook") || titleLower.contains("ultrabook") || titleLower.contains("chromebook")) {
            addTags(tags, "#laptop", "#techdeals", "#workfromhome", "#productivity", "#studentdeals", "#laptopoffer");
        }

        // Tablets & iPads
        if (titleLower.contains("ipad") || titleLower.contains("tablet") || titleLower.contains("tab")) {
            addTags(tags, "#tablet", "#ipad", "#digitalart", "#tabletoffer", "#edtech", "#portabletech");
        }

        // PC Accessories & Peripherals
        if (titleLower.contains("keyboard") || titleLower.contains("mouse") || titleLower.contains("monitor")
                || titleLower.contains("ssd") || titleLower.contains("hard drive") || titleLower.contains("pendrive")
                || titleLower.contains("power bank") || titleLower.contains("charger") || titleLower.contains("cable")) {
            addTags(tags, "#pcaccessories", "#desksetup", "#powerbank", "#fastcharger", "#gadgets", "#techaccessories");
        }

        // Gaming Consoles & Accessories
        if (titleLower.contains("ps5") || titleLower.contains("playstation") || titleLower.contains("xbox")
                || titleLower.contains("gaming") || titleLower.contains("controller") || titleLower.contains("gamepad")) {
            addTags(tags, "#gaming", "#ps5", "#xbox", "#gamerlife", "#gamingcommunity", "#gaminggear");
        }

        // Smart TVs & Projectors
        if (titleLower.contains("tv") || titleLower.contains("television") || titleLower.contains("smart tv")
                || titleLower.contains("4k") || titleLower.contains("oled") || titleLower.contains("qled") || titleLower.contains("projector")) {
            addTags(tags, "#smarttv", "#4ktv", "#androidtv", "#homecinema", "#bingewatching", "#electronics");
        }

        // Cameras & Photography
        if (titleLower.contains("camera") || titleLower.contains("dslr") || titleLower.contains("mirrorless")
                || titleLower.contains("gopro") || titleLower.contains("action cam") || titleLower.contains("tripod") || titleLower.contains("vlogging")) {
            addTags(tags, "#camera", "#dslr", "#photography", "#vlogging", "#actioncam", "#contentcreator");
        }

        // Footwear / Shoes / Sneakers
        if (titleLower.contains("sneaker") || titleLower.contains("shoe") || titleLower.contains("running shoe")
                || titleLower.contains("sandal") || titleLower.contains("slipper") || titleLower.contains("crocs") || titleLower.contains("loafer")) {
            addTags(tags, "#shoes", "#sneakers", "#footwear", "#sneakerhead", "#kicks", "#comfortwear", "#shoeaddict");
        }

        // Men's Fashion & Apparel
        if (titleLower.contains("t-shirt") || titleLower.contains("tshirt") || titleLower.contains("shirt")
                || titleLower.contains("polo") || titleLower.contains("hoodie") || titleLower.contains("jacket")
                || titleLower.contains("jeans") || titleLower.contains("trouser") || titleLower.contains("joggers")) {
            addTags(tags, "#mensfashion", "#streetwear", "#menswear", "#outfitinspo", "#ootdmen", "#casualwear");
        }

        // Women's Fashion & Ethnic Wear
        if (titleLower.contains("saree") || titleLower.contains("kurti") || titleLower.contains("kurta")
                || titleLower.contains("dress") || titleLower.contains("lehenga") || titleLower.contains("top")
                || titleLower.contains("gown") || titleLower.contains("legging")) {
            addTags(tags, "#ethnicwear", "#sareelove", "#kurti", "#dresses", "#womensfashion", "#fashioninspo", "#ootd");
        }

        // Bags, Backpacks & Luggage
        if (titleLower.contains("backpack") || titleLower.contains("bag") || titleLower.contains("handbag")
                || titleLower.contains("wallet") || titleLower.contains("luggage") || titleLower.contains("trolley") || titleLower.contains("suitcase")) {
            addTags(tags, "#backpack", "#travelbag", "#handbags", "#luggage", "#everydaycarry", "#travelessentials");
        }

        // Kitchen Appliances (Air Fryer, Mixer, Cooker, Kettle)
        if (titleLower.contains("air fryer") || titleLower.contains("fryer")) {
            addTags(tags, "#airfryer", "#healthycooking", "#airfryerrecipes", "#kitchengadgets", "#cookinghacks");
        }
        if (titleLower.contains("mixer") || titleLower.contains("grinder") || titleLower.contains("blender")
                || titleLower.contains("juicer") || titleLower.contains("chopper")) {
            addTags(tags, "#mixergrinder", "#blender", "#kitchenappliances", "#foodprep", "#smartkitchen");
        }
        if (titleLower.contains("cooker") || titleLower.contains("pan") || titleLower.contains("kadai")
                || titleLower.contains("kettle") || titleLower.contains("cookware") || titleLower.contains("induction") || titleLower.contains("gas stove")) {
            addTags(tags, "#cookware", "#nonstick", "#pressurecooker", "#kitchenessentials", "#homekitchen");
        }
        if (titleLower.contains("bottle") || titleLower.contains("flask") || titleLower.contains("thermos") || titleLower.contains("lunch box")) {
            addTags(tags, "#waterbottle", "#thermosflask", "#hydration", "#fitnessbottle", "#kitchenware");
        }

        // Home Decor, Furniture & Bedding
        if (titleLower.contains("bedsheet") || titleLower.contains("curtain") || titleLower.contains("pillow")
                || titleLower.contains("blanket") || titleLower.contains("mattress") || titleLower.contains("comforter")) {
            addTags(tags, "#bedsheet", "#homedecor", "#bedding", "#bedroomgoals", "#comfortliving", "#homefinds");
        }
        if (titleLower.contains("chair") || titleLower.contains("desk") || titleLower.contains("table")
                || titleLower.contains("sofa") || titleLower.contains("lamp") || titleLower.contains("led light")) {
            addTags(tags, "#homedecor", "#interiorstyling", "#desksetup", "#furniture", "#ergonomic", "#lighting");
        }

        // Home Appliances (Vacuum, Iron, Fan, Purifier, AC, Geyser)
        if (titleLower.contains("vacuum") || titleLower.contains("iron") || titleLower.contains("fan")
                || titleLower.contains("purifier") || titleLower.contains("geyser") || titleLower.contains("cooler")) {
            addTags(tags, "#homeappliances", "#smartliving", "#cleanhome", "#homecare", "#appliances");
        }

        // Skincare & Beauty
        if (titleLower.contains("serum") || titleLower.contains("sunscreen") || titleLower.contains("moisturizer")
                || titleLower.contains("face wash") || titleLower.contains("cream") || titleLower.contains("lotion") || titleLower.contains("mask")) {
            addTags(tags, "#skincare", "#skincareroutine", "#faceserum", "#sunscreen", "#glowingskin", "#beautyhacks");
        }
        if (titleLower.contains("shampoo") || titleLower.contains("conditioner") || titleLower.contains("hair oil")
                || titleLower.contains("hair dryer") || titleLower.contains("straightener")) {
            addTags(tags, "#haircare", "#hairroutine", "#hairdryer", "#hairgoals", "#healthyhair");
        }
        if (titleLower.contains("lipstick") || titleLower.contains("makeup") || titleLower.contains("foundation")
                || titleLower.contains("eyeliner") || titleLower.contains("mascara")) {
            addTags(tags, "#makeup", "#cosmetics", "#lipstick", "#beautyessentials", "#glamlook");
        }
        if (titleLower.contains("trimmer") || titleLower.contains("shaver") || titleLower.contains("beard") || titleLower.contains("grooming")) {
            addTags(tags, "#mensgrooming", "#beardcare", "#trimmer", "#shaver", "#groomingessentials");
        }
        if (titleLower.contains("perfume") || titleLower.contains("deodorant") || titleLower.contains("fragrance") || titleLower.contains("cologne")) {
            addTags(tags, "#perfume", "#fragrance", "#deodorant", "#scentoftheday", "#luxuryperfume");
        }

        // Health, Fitness & Supplements
        if (titleLower.contains("whey") || titleLower.contains("protein") || titleLower.contains("creatine")
                || titleLower.contains("supplement") || titleLower.contains("peanut butter") || titleLower.contains("multivitamin")) {
            addTags(tags, "#fitness", "#wheyprotein", "#bodybuilding", "#gymsupplements", "#healthyroutine", "#fitlife");
        }
        if (titleLower.contains("yoga mat") || titleLower.contains("dumbbell") || titleLower.contains("gym")
                || titleLower.contains("massager") || titleLower.contains("scale") || titleLower.contains("bp monitor")) {
            addTags(tags, "#homegym", "#workoutgear", "#yogalife", "#fitnessmotivation", "#healthmonitor");
        }

        // Sports & Outdoor
        if (titleLower.contains("cricket") || titleLower.contains("badminton") || titleLower.contains("football")
                || titleLower.contains("bicycle") || titleLower.contains("cycle") || titleLower.contains("sports")) {
            addTags(tags, "#sportsgear", "#cricket", "#badminton", "#cycling", "#outdoorsports");
        }

        // Baby Care & Kids Toys
        if (titleLower.contains("toy") || titleLower.contains("drone") || titleLower.contains("lego")
                || titleLower.contains("rc car") || titleLower.contains("puzzle") || titleLower.contains("diaper") || titleLower.contains("baby")) {
            addTags(tags, "#kidstoys", "#babycare", "#babyessentials", "#rctoys", "#parentinghacks");
        }

        // Books & Stationery
        if (titleLower.contains("book") || titleLower.contains("novel") || titleLower.contains("diary")
                || titleLower.contains("manga") || titleLower.contains("notebook") || titleLower.contains("pen")) {
            addTags(tags, "#bookstagram", "#readinglist", "#selfhelpbooks", "#bookworm", "#stationery");
        }

        // Automotive & Car Accessories
        if (titleLower.contains("car") || titleLower.contains("dashcam") || titleLower.contains("tyre")
                || titleLower.contains("inflator") || titleLower.contains("mobile holder") || titleLower.contains("bike")) {
            addTags(tags, "#caraccessories", "#dashcam", "#cardetailing", "#autocare", "#bikerslife");
        }
    }

    /**
     * Adds price and discount specific hashtags.
     */
    private void extractPriceAndDiscountTags(String titleLower, String price, String mrp, String discount, Set<String> tags) {
        // High discount tags
        int discNum = parseDiscount(discount, price, mrp);
        if (discNum >= 60) {
            addTags(tags, "#stealdeal", "#lootdeal", "#megasale", "#superdiscount", "#hugepricecut");
        } else if (discNum >= 40) {
            addTags(tags, "#discountdeals", "#pricedrop", "#salealert", "#bigsaving");
        } else if (discNum >= 20) {
            addTags(tags, "#specialoffer", "#dealsalert");
        }

        // Price bracket tags
        if (price != null && !price.isEmpty()) {
            try {
                double numPrice = Double.parseDouble(price.replaceAll("[^0-9.]", ""));
                if (numPrice > 0 && numPrice <= 499) {
                    addTags(tags, "#under500", "#budgetfinds", "#pocketfriendly", "#supercheap");
                } else if (numPrice <= 999) {
                    addTags(tags, "#under1000", "#budgetdeals", "#valueformoney");
                } else if (numPrice <= 1999) {
                    addTags(tags, "#under2000", "#bestvalue");
                } else if (numPrice <= 4999) {
                    addTags(tags, "#under5000", "#smartshopping");
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * Category Fallback Hashtags when product title keywords are sparse.
     */
    private void addCategoryFallbackTags(ProductCategory category, Set<String> tags) {
        if (category == null) {
            addTags(tags, "#amazongadgets", "#trendingfinds", "#usefulproducts");
            return;
        }

        switch (category) {
            case WATCH:
                addTags(tags, "#smartwatch", "#watchcollector", "#watchlover", "#wristwear", "#mensfashion");
                break;
            case TV:
                addTags(tags, "#smarttv", "#androidtv", "#electronics", "#homeentertainment", "#4k");
                break;
            case MOBILE:
                addTags(tags, "#smartphone", "#mobiledeals", "#gadgets", "#technology", "#5gphone");
                break;
            case LAPTOP:
                addTags(tags, "#laptop", "#gaminglaptop", "#studentdeals", "#workfromhome", "#techdeals");
                break;
            case HEADPHONE:
                addTags(tags, "#earbuds", "#bluetooth", "#headphones", "#music", "#audiophile");
                break;
            case SPEAKER:
                addTags(tags, "#bluetoothspeaker", "#soundbar", "#partybox", "#homeaudio");
                break;
            case CAMERA:
                addTags(tags, "#camera", "#dslr", "#photography", "#vlogging", "#actioncam");
                break;
            case SHOE:
                addTags(tags, "#shoes", "#sneakers", "#fashion", "#sneakerhead", "#amazonfashion");
                break;
            case SHIRT:
            case DRESS:
                addTags(tags, "#fashion", "#style", "#ootd", "#outfitinspo", "#amazonfashion", "#streetwear");
                break;
            case KITCHEN:
                addTags(tags, "#kitchen", "#kitchengadgets", "#homekitchen", "#appliances", "#cookinghacks");
                break;
            case HOME:
                addTags(tags, "#homedecor", "#homekitchen", "#interior", "#appliances", "#homefinds");
                break;
            case BEAUTY:
                addTags(tags, "#beauty", "#skincare", "#makeup", "#selfcare", "#grooming");
                break;
            case HEALTH:
                addTags(tags, "#fitness", "#gymlife", "#healthylifestyle", "#supplements", "#wellness");
                break;
            case SPORTS:
                addTags(tags, "#sportsgear", "#cricket", "#badminton", "#cycling", "#workoutgear");
                break;
            case BOOK:
                addTags(tags, "#books", "#reading", "#bookstagram", "#novel", "#selfhelp");
                break;
            case TOY:
                addTags(tags, "#toys", "#kidstoys", "#babycare", "#rctoys", "#games");
                break;
            default:
                addTags(tags, "#amazongadgets", "#trendingfinds", "#usefulproducts");
                break;
        }
    }

    /**
     * High-performing viral and deal community hashtags.
     */
    private void addViralDealTags(Set<String> tags) {
        addTags(tags,
            "#amazonfinds", "#amazonmusthaves", "#amazondeals", "#dealsoftheday",
            "#lootdeal", "#budgetfinds", "#salealert", "#offerzone2538"
        );
    }

    /**
     * Extracts a single top product keyword tag for compact telegram/facebook posts.
     */
    private void extractTopProductKeywordTag(String titleLower, Set<String> tags) {
        if (titleLower.contains("earbuds") || titleLower.contains("tws") || titleLower.contains("airpods")) {
            tags.add("#Earbuds");
        } else if (titleLower.contains("headphone")) {
            tags.add("#Headphones");
        } else if (titleLower.contains("speaker") || titleLower.contains("soundbar")) {
            tags.add("#Speaker");
        } else if (titleLower.contains("smartwatch") || titleLower.contains("watch")) {
            tags.add("#Smartwatch");
        } else if (titleLower.contains("laptop") || titleLower.contains("macbook")) {
            tags.add("#Laptop");
        } else if (titleLower.contains("iphone") || titleLower.contains("phone") || titleLower.contains("mobile")) {
            tags.add("#Smartphone");
        } else if (titleLower.contains("sneaker") || titleLower.contains("shoe")) {
            tags.add("#Sneakers");
        } else if (titleLower.contains("shirt") || titleLower.contains("t-shirt")) {
            tags.add("#Fashion");
        } else if (titleLower.contains("saree") || titleLower.contains("kurti") || titleLower.contains("dress")) {
            tags.add("#EthnicWear");
        } else if (titleLower.contains("air fryer") || titleLower.contains("mixer") || titleLower.contains("cooker")) {
            tags.add("#KitchenDeals");
        } else if (titleLower.contains("serum") || titleLower.contains("sunscreen") || titleLower.contains("skincare")) {
            tags.add("#Skincare");
        } else if (titleLower.contains("protein") || titleLower.contains("gym")) {
            tags.add("#Fitness");
        }
    }

    private void addTags(Set<String> set, String... tags) {
        set.addAll(Arrays.asList(tags));
    }

    private int parseDiscount(String discount, String price, String mrp) {
        if (discount != null && !discount.isEmpty()) {
            try {
                String clean = discount.replaceAll("[^0-9]", "");
                if (!clean.isEmpty()) return Integer.parseInt(clean);
            } catch (Exception ignored) {}
        }
        if (price != null && mrp != null) {
            try {
                double p = Double.parseDouble(price.replaceAll("[^0-9.]", ""));
                double m = Double.parseDouble(mrp.replaceAll("[^0-9.]", ""));
                if (m > p && m > 0) {
                    return (int) Math.round(((m - p) / m) * 100.0);
                }
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}