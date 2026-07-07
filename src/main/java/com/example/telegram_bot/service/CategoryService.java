package com.example.telegram_bot.service;

import org.springframework.stereotype.Service;

import com.example.telegram_bot.model.ProductCategory;

@Service
public class CategoryService {

    public ProductCategory detectCategory(String title) {

        String product = title.toLowerCase();

        // Watches
        if (product.contains("watch")) {
            return ProductCategory.WATCH;
        }

        // Mobiles
        if (product.contains("phone")
                || product.contains("iphone")
                || product.contains("mobile")
                || product.contains("smartphone")) {

            return ProductCategory.MOBILE;
        }

        // Laptop
        if (product.contains("laptop")
                || product.contains("notebook")
                || product.contains("macbook")) {

            return ProductCategory.LAPTOP;
        }

        // TV
        if (product.contains("tv")
                || product.contains("android tv")
                || product.contains("qled")
                || product.contains("oled")) {

            return ProductCategory.TV;
        }

        // Audio
        if (product.contains("earbuds")
                || product.contains("headphone")
                || product.contains("earphone")) {

            return ProductCategory.HEADPHONE;
        }

        // Fashion
        if (product.contains("shoe")
                || product.contains("sneaker")) {

            return ProductCategory.SHOE;
        }

        if (product.contains("shirt")
                || product.contains("t-shirt")) {

            return ProductCategory.SHIRT;
        }

        if (product.contains("dress")) {

            return ProductCategory.DRESS;
        }

        // Kitchen
        if (product.contains("mixer")
                || product.contains("pressure cooker")
                || product.contains("cookware")
                || product.contains("pan")) {

            return ProductCategory.KITCHEN;
        }

        // Home
        if (product.contains("chair")
                || product.contains("table")
                || product.contains("sofa")) {

            return ProductCategory.HOME;
        }

        // Beauty
        if (product.contains("cream")
                || product.contains("face wash")
                || product.contains("shampoo")) {

            return ProductCategory.BEAUTY;
        }

        // Books
        if (product.contains("book")) {

            return ProductCategory.BOOK;
        }

        // Toys
        if (product.contains("toy")) {

            return ProductCategory.TOY;
        }

        return ProductCategory.DEFAULT;
    }

}