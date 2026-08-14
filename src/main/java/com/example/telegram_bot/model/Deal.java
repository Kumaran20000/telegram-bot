package com.example.telegram_bot.model;

public class Deal {

    private String title;
    private String price;
    private String mrp;
    private String discount;
    private String image;
    private String link;
    private String source;

    // Deal Score Breakdown fields
    private double dealScore;
    private double discountScore;
    private double priceAttractivenessScore;
    private double productPopularityScore;
    private double categoryDemandScore;
    private double previousPerformanceScore;

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

    public double getDealScore() { return dealScore; }
    public void setDealScore(double dealScore) { this.dealScore = dealScore; }

    public double getDiscountScore() { return discountScore; }
    public void setDiscountScore(double discountScore) { this.discountScore = discountScore; }

    public double getPriceAttractivenessScore() { return priceAttractivenessScore; }
    public void setPriceAttractivenessScore(double priceAttractivenessScore) { this.priceAttractivenessScore = priceAttractivenessScore; }

    public double getProductPopularityScore() { return productPopularityScore; }
    public void setProductPopularityScore(double productPopularityScore) { this.productPopularityScore = productPopularityScore; }

    public double getCategoryDemandScore() { return categoryDemandScore; }
    public void setCategoryDemandScore(double categoryDemandScore) { this.categoryDemandScore = categoryDemandScore; }

    public double getPreviousPerformanceScore() { return previousPerformanceScore; }
    public void setPreviousPerformanceScore(double previousPerformanceScore) { this.previousPerformanceScore = previousPerformanceScore; }

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

    public long calculateSavingsAmount() {
        if (price != null && mrp != null) {
            try {
                double p = Double.parseDouble(price.replaceAll("[^0-9.]", ""));
                double m = Double.parseDouble(mrp.replaceAll("[^0-9.]", ""));
                if (m > p) {
                    return Math.round(m - p);
                }
            } catch (Exception ignored) {}
        }
        return 0;
    }

    /**
     * Calculates total Deal Score:
     * Deal Score = Discount % + Price Attractiveness + Product Popularity + Category Demand + Previous Performance Score
     */
    public double computeDealScore(ProductCategory category, double prevPerfScore) {
        // 1. Discount Score (0-100 pts)
        this.discountScore = Math.min(calculateDiscountPercent(), 100);

        // 2. Price Attractiveness Score (0-30 pts)
        double pScore = 10;
        try {
            if (price != null && !price.isEmpty()) {
                double numPrice = Double.parseDouble(price.replaceAll("[^0-9.]", ""));
                if (numPrice <= 500) {
                    pScore = 30; // Impulse buy sweet spot
                } else if (numPrice <= 1500) {
                    pScore = 25;
                } else if (numPrice <= 3500) {
                    pScore = 20;
                } else if (numPrice <= 8000) {
                    pScore = 15;
                } else {
                    pScore = 10;
                }
            }
        } catch (Exception ignored) {}
        this.priceAttractivenessScore = pScore;

        // 3. Product Popularity Score (0-25 pts)
        double popScore = 5;
        if (title != null) {
            String lowerTitle = title.toLowerCase();
            if (lowerTitle.contains("apple") || lowerTitle.contains("samsung") || lowerTitle.contains("sony") ||
                lowerTitle.contains("boat") || lowerTitle.contains("noise") || lowerTitle.contains("oneplus") ||
                lowerTitle.contains("nike") || lowerTitle.contains("adidas") || lowerTitle.contains("puma") ||
                lowerTitle.contains("dell") || lowerTitle.contains("hp") || lowerTitle.contains("asus") ||
                lowerTitle.contains("lenovo") || lowerTitle.contains("realme") || lowerTitle.contains("redmi")) {
                popScore += 15;
            }
            if (lowerTitle.contains("bestseller") || lowerTitle.contains("pro") || lowerTitle.contains("ultra") || lowerTitle.contains("wireless") || lowerTitle.contains("smartwatch") || lowerTitle.contains("earbuds")) {
                popScore += 5;
            }
        }
        this.productPopularityScore = Math.min(popScore, 25);

        // 4. Category Demand Score (0-25 pts)
        double catScore = 10;
        if (category != null) {
            switch (category) {
                case HEADPHONE:
                case MOBILE:
                    catScore = 25;
                    break;
                case WATCH:
                case LAPTOP:
                    catScore = 20;
                    break;
                case SHOE:
                    catScore = 15;
                    break;
                case HOME:
                    catScore = 12;
                    break;
                default:
                    catScore = 10;
                    break;
            }
        }
        this.categoryDemandScore = catScore;

        // 5. Previous Performance Score (0-20 pts)
        this.previousPerformanceScore = Math.min(Math.max(prevPerfScore, 0), 20);

        // Total Deal Score
        this.dealScore = this.discountScore + this.priceAttractivenessScore + this.productPopularityScore + this.categoryDemandScore + this.previousPerformanceScore;
        return this.dealScore;
    }

    public String getDealRatingBadge() {
        int disc = calculateDiscountPercent();
        long savings = calculateSavingsAmount();
        String savingsText = savings > 0 ? " | SAVE ₹" + String.format("%,d", savings) : "";

        if (disc >= 60) {
            return "🔥 SUPER STEAL DEAL (" + disc + "% OFF" + savingsText + ") 🌟🌟🌟🌟🌟";
        } else if (disc >= 40) {
            return "⚡ MEGA DISCOUNT (" + disc + "% OFF" + savingsText + ") ⭐⭐⭐⭐";
        } else if (disc >= 20) {
            return "💥 HOT OFFER (" + disc + "% OFF" + savingsText + ") ⭐⭐⭐";
        } else if (disc > 0) {
            return "✨ SPECIAL DEAL (" + disc + "% OFF" + savingsText + ") ⭐⭐⭐";
        } else {
            return "🔥 HOT DEAL ALERT ⭐⭐⭐⭐";
        }
    }
}
