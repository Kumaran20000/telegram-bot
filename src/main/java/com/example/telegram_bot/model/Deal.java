package com.example.telegram_bot.model;

public class Deal {

    private String title;
    private String price;
    private String mrp;
    private String discount;
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

    public String getMrp() { return mrp; }
    public void setMrp(String mrp) { this.mrp = mrp; }

    public String getDiscount() { return discount; }
    public void setDiscount(String discount) { this.discount = discount; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public int calculateDiscountPercent() {
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

    public String getDealRatingBadge() {
        int disc = calculateDiscountPercent();
        if (disc >= 60) {
            return "🔥 SUPER DEAL (" + disc + "% OFF) 🌟🌟🌟🌟🌟";
        } else if (disc >= 40) {
            return "⚡ HOT DEAL (" + disc + "% OFF) ⭐⭐⭐⭐";
        } else if (disc >= 20) {
            return "💥 GOOD DEAL (" + disc + "% OFF) ⭐⭐⭐";
        } else if (disc > 0) {
            return "✨ SPECIAL OFFER (" + disc + "% OFF) ⭐⭐⭐";
        } else {
            return "🔥 HOT DEAL ALERT ⭐⭐⭐⭐";
        }
    }
}
