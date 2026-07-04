package com.example.telegram_bot.model;

public class Product {

    private String name;
    private String price;
    private String link;

    public Product() {
    }

    public Product(String name, String price, String link) {
        this.name = name;
        this.price = price;
        this.link = link;
    }

    public String getName() { return name; }
    public String getPrice() { return price; }
    public String getLink() { return link; }
}
