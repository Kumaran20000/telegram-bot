package com.example.telegram_bot.service;

import org.springframework.stereotype.Service;

import com.example.telegram_bot.model.ProductCategory;

@Service
public class CategoryService {

    public ProductCategory detectCategory(String title) {
        if (title == null || title.trim().isEmpty()) {
            return ProductCategory.DEFAULT;
        }

        String product = title.toLowerCase();

        // 1. Audio / Bluetooth Headphones / Earbuds
        if (product.contains("earbuds")
                || product.contains("headphone")
                || product.contains("earphone")
                || product.contains("tws")
                || product.contains("neckband")
                || product.contains("airpods")
                || product.contains("airdopes")
                || product.contains("headset")
                || product.contains("earpod")
                || product.contains("over ear")
                || product.contains("in-ear")
                || product.contains("in ear")) {
            return ProductCategory.HEADPHONE;
        }

        // 2. Speakers & Soundbars
        if (product.contains("soundbar")
                || product.contains("sound bar")
                || product.contains("speaker")
                || product.contains("partybox")
                || product.contains("home theatre")
                || product.contains("woofer")
                || product.contains("subwoofer")
                || product.contains("echo dot")
                || product.contains("alexa")) {
            return ProductCategory.SPEAKER;
        }

        // 3. Watches & Wearables
        if (product.contains("smartwatch")
                || product.contains("smart watch")
                || product.contains("fitness band")
                || product.contains("smart band")
                || product.contains("watch")
                || product.contains("chronograph")
                || product.contains("fitbit")) {
            return ProductCategory.WATCH;
        }

        // 4. Mobiles & Smartphones
        if (product.contains("iphone")
                || product.contains("smartphone")
                || product.contains("mobile")
                || product.contains("phone")
                || product.contains("samsung galaxy")
                || product.contains("oneplus")
                || product.contains("redmi")
                || product.contains("realme")
                || product.contains("poco")
                || product.contains("5g phone")) {
            return ProductCategory.MOBILE;
        }

        // 5. Laptops & Computers
        if (product.contains("laptop")
                || product.contains("macbook")
                || product.contains("notebook")
                || product.contains("chromebook")
                || product.contains("ultrabook")
                || product.contains("thinkpad")
                || product.contains("ideapad")
                || product.contains("gaming laptop")
                || product.contains("zenbook")
                || product.contains("vivobook")) {
            return ProductCategory.LAPTOP;
        }

        // 6. TV & Home Entertainment & Monitors
        if (product.contains("smart tv")
                || product.contains("android tv")
                || product.contains("qled")
                || product.contains("oled")
                || product.contains("led tv")
                || product.contains("television")
                || product.contains("tv")
                || product.contains("projector")
                || product.contains("monitor")) {
            return ProductCategory.TV;
        }

        // 7. Cameras & Photography
        if (product.contains("camera")
                || product.contains("dslr")
                || product.contains("mirrorless")
                || product.contains("gopro")
                || product.contains("action cam")
                || product.contains("webcam")
                || product.contains("tripod")
                || product.contains("ring light")
                || product.contains("gimbal")) {
            return ProductCategory.CAMERA;
        }

        // 8. Shoes & Footwear
        if (product.contains("shoe")
                || product.contains("sneaker")
                || product.contains("footwear")
                || product.contains("running shoe")
                || product.contains("sandal")
                || product.contains("slipper")
                || product.contains("loafer")
                || product.contains("crocs")
                || product.contains("clog")
                || product.contains("boots")
                || product.contains("flip flop")) {
            return ProductCategory.SHOE;
        }

        // 9. Women's Fashion & Ethnic Wear
        if (product.contains("saree")
                || product.contains("kurti")
                || product.contains("kurta")
                || product.contains("dress")
                || product.contains("lehenga")
                || product.contains("gown")
                || product.contains("jumpsuit")
                || product.contains("skirt")
                || product.contains("legging")
                || product.contains("ethnic wear")
                || product.contains("top")) {
            return ProductCategory.DRESS;
        }

        // 10. Men's Fashion & Clothing
        if (product.contains("shirt")
                || product.contains("t-shirt")
                || product.contains("tshirt")
                || product.contains("polo")
                || product.contains("hoodie")
                || product.contains("jacket")
                || product.contains("sweatshirt")
                || product.contains("jeans")
                || product.contains("trouser")
                || product.contains("pant")
                || product.contains("joggers")
                || product.contains("shorts")
                || product.contains("blazer")
                || product.contains("trackpant")) {
            return ProductCategory.SHIRT;
        }

        // 11. Kitchen & Cooking Appliances & Cookware
        if (product.contains("air fryer")
                || product.contains("fryer")
                || product.contains("mixer")
                || product.contains("grinder")
                || product.contains("blender")
                || product.contains("juicer")
                || product.contains("pressure cooker")
                || product.contains("cookware")
                || product.contains("pan")
                || product.contains("kadai")
                || product.contains("tawa")
                || product.contains("kettle")
                || product.contains("microwave")
                || product.contains("toaster")
                || product.contains("oven")
                || product.contains("water purifier")
                || product.contains("water bottle")
                || product.contains("flask")
                || product.contains("chopper")
                || product.contains("lunch box")
                || product.contains("gas stove")
                || product.contains("induction")
                || product.contains("coffee maker")) {
            return ProductCategory.KITCHEN;
        }

        // 12. Home, Living, Furniture & Bedding
        if (product.contains("bedsheet")
                || product.contains("blanket")
                || product.contains("pillow")
                || product.contains("mattress")
                || product.contains("comforter")
                || product.contains("curtain")
                || product.contains("chair")
                || product.contains("desk")
                || product.contains("table")
                || product.contains("sofa")
                || product.contains("lamp")
                || product.contains("light")
                || product.contains("strip light")
                || product.contains("decor")
                || product.contains("clock")
                || product.contains("vacuum")
                || product.contains("iron")
                || product.contains("fan")
                || product.contains("cooler")
                || product.contains("geyser")
                || product.contains("heater")) {
            return ProductCategory.HOME;
        }

        // 13. Beauty, Skincare, Makeup & Grooming
        if (product.contains("serum")
                || product.contains("sunscreen")
                || product.contains("moisturizer")
                || product.contains("face wash")
                || product.contains("cream")
                || product.contains("lotion")
                || product.contains("shampoo")
                || product.contains("conditioner")
                || product.contains("hair oil")
                || product.contains("hair dryer")
                || product.contains("straightener")
                || product.contains("trimmer")
                || product.contains("shaver")
                || product.contains("lipstick")
                || product.contains("makeup")
                || product.contains("perfume")
                || product.contains("deodorant")
                || product.contains("fragrance")
                || product.contains("skincare")) {
            return ProductCategory.BEAUTY;
        }

        // 14. Health, Fitness & Nutrition
        if (product.contains("whey")
                || product.contains("protein")
                || product.contains("creatine")
                || product.contains("supplement")
                || product.contains("multivitamin")
                || product.contains("peanut butter")
                || product.contains("massager")
                || product.contains("bp monitor")
                || product.contains("oximeter")
                || product.contains("weighing scale")
                || product.contains("thermometer")
                || product.contains("health monitor")) {
            return ProductCategory.HEALTH;
        }

        // 15. Sports & Outdoor
        if (product.contains("dumbbell")
                || product.contains("gym")
                || product.contains("yoga mat")
                || product.contains("resistance band")
                || product.contains("cricket")
                || product.contains("badminton")
                || product.contains("racket")
                || product.contains("football")
                || product.contains("cycle")
                || product.contains("bicycle")
                || product.contains("sports")) {
            return ProductCategory.SPORTS;
        }

        // 16. Books & Stationery
        if (product.contains("book")
                || product.contains("novel")
                || product.contains("diary")
                || product.contains("manga")
                || product.contains("notebook")
                || product.contains("comic")
                || product.contains("paperback")
                || product.contains("hardcover")
                || product.contains("stationery")) {
            return ProductCategory.BOOK;
        }

        // 17. Toys & Baby Care
        if (product.contains("toy")
                || product.contains("drone")
                || product.contains("lego")
                || product.contains("board game")
                || product.contains("puzzle")
                || product.contains("doll")
                || product.contains("rc car")
                || product.contains("remote control")
                || product.contains("diaper")
                || product.contains("baby")
                || product.contains("stroller")
                || product.contains("cradle")
                || product.contains("wipes")) {
            return ProductCategory.TOY;
        }

        return ProductCategory.DEFAULT;
    }

}