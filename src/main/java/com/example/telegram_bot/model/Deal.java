package com.example.telegram_bot.model;

public class Deal {

    private String title;
    private String price;
    private String image;
    private String link;
    private String source;

    public Deal() {}

    public Deal(String title, String price, String image, String link, String source) {
        this.title = title;
        this.price = price;
        this.image = image;
        this.link = link;
        this.source = source;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
