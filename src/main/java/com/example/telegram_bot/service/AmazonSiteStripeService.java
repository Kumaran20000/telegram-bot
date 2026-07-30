package com.example.telegram_bot.service;

import com.example.telegram_bot.model.Deal;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AmazonSiteStripeService {

    private final GoogleSheetService googleSheetService;

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    private static final Pattern ASIN_PATTERN = Pattern.compile("/(?:dp|gp/product)/([A-Z0-9]{10})");

    public AmazonSiteStripeService(GoogleSheetService googleSheetService) {
        this.googleSheetService = googleSheetService;
    }

    /**
     * Accepts a raw URL, shortlink (amzn.to), or SiteStripe HTML snippet,
     * extracts missing product details from Amazon, saves the deal to Google Sheet,
     * and returns the populated Deal.
     */
    public Deal processAndSaveSiteStripe(String rawInput, String customTitle, String customPrice, String customImage) throws Exception {
        String url = extractUrl(rawInput);
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("No valid URL found in input.");
        }

        // Expand short URLs if needed (e.g. amzn.to)
        String finalUrl = expandUrl(url);

        Deal scrapedDeal = scrapeAmazonProduct(finalUrl);

        // Override with custom fields if provided
        if (customTitle != null && !customTitle.trim().isEmpty()) {
            scrapedDeal.setTitle(customTitle.trim());
        }
        if (customPrice != null && !customPrice.trim().isEmpty()) {
            scrapedDeal.setPrice(cleanPrice(customPrice));
        }
        if (customImage != null && !customImage.trim().isEmpty()) {
            scrapedDeal.setImage(customImage.trim());
        }

        // Preserve original SiteStripe affiliate link if provided, or use expanded URL
        scrapedDeal.setLink(url);

        // Save to Google Sheet
        googleSheetService.saveDeal(scrapedDeal);

        return scrapedDeal;
    }

    /**
     * Scans existing rows in Google Sheet for any row with a link but missing product details (title, price, or image),
     * scrapes Amazon for those details, and updates the row in Google Sheet.
     */
    public int enrichSheetDeals() throws Exception {
        var rows = googleSheetService.getAllRows();
        if (rows == null || rows.isEmpty()) {
            return 0;
        }

        int updatedCount = 0;
        int rowIndex = 2; // Data starts at row 2 in Sheet1

        for (var row : rows) {
            String title = row.size() > 0 ? row.get(0).toString().trim() : "";
            String price = row.size() > 1 ? row.get(1).toString().trim() : "";
            String image = row.size() > 2 ? row.get(2).toString().trim() : "";
            String link = row.size() > 3 ? row.get(3).toString().trim() : "";

            // If link exists and any detail is missing or incomplete
            if (!link.isEmpty() && (title.isEmpty() || price.isEmpty() || image.isEmpty() || title.equalsIgnoreCase("Amazon Deal") || price.equalsIgnoreCase("N/A"))) {
                System.out.println("Enriching Google Sheet Row " + rowIndex + " for link: " + link);
                String expanded = expandUrl(extractUrl(link));
                Deal scraped = scrapeAmazonProduct(expanded);

                Deal updatedDeal = new Deal();
                updatedDeal.setTitle(!title.isEmpty() && !title.equalsIgnoreCase("Amazon Deal") ? title : scraped.getTitle());
                updatedDeal.setPrice(!price.isEmpty() && !price.equalsIgnoreCase("N/A") ? price : scraped.getPrice());
                updatedDeal.setImage(!image.isEmpty() ? image : scraped.getImage());
                updatedDeal.setLink(link);
                updatedDeal.setSource(row.size() > 4 && !row.get(4).toString().trim().isEmpty() ? row.get(4).toString().trim() : "Amazon");

                googleSheetService.updateDealRow(rowIndex, updatedDeal);
                updatedCount++;
            }
            rowIndex++;
        }

        return updatedCount;
    }

    /**
     * Scrapes top N deal offers from Amazon Goldbox / Today's Deals (https://www.amazon.in/gp/goldbox),
     * extracts their details (Title, Price, Image, Link), and saves them all to Google Sheet.
     */
    public List<Deal> scrapeGoldboxTopDeals(String goldboxUrl, int limit) throws Exception {
        Set<String> productUrls = new LinkedHashSet<>();

        String targetUrl = (goldboxUrl != null && !goldboxUrl.trim().isEmpty())
                ? goldboxUrl.trim()
                : "https://www.amazon.in/gp/goldbox";

        String[] sources = new String[]{
                targetUrl,
                "https://www.amazon.in/deals",
                "https://www.amazon.in/gp/bestsellers",
                "https://www.amazon.in/gp/movers-and-shakers"
        };

        for (String source : sources) {
            if (productUrls.size() >= limit) break;
            try {
                Document doc = Jsoup.connect(source)
                        .userAgent(USER_AGENT)
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                        .referrer("https://www.google.com")
                        .timeout(10000)
                        .followRedirects(true)
                        .get();

                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    String href = link.attr("abs:href");
                    if (href.contains("/dp/") || href.contains("/gp/product/")) {
                        Matcher matcher = ASIN_PATTERN.matcher(href);
                        if (matcher.find()) {
                            String asin = matcher.group(1);
                            String cleanUrl = "https://www.amazon.in/dp/" + asin;
                            productUrls.add(cleanUrl);
                            if (productUrls.size() >= limit) break;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Warning: Failed to fetch deals from source [" + source + "]: " + e.getMessage());
            }
        }

        List<Deal> savedDeals = new ArrayList<>();
        int count = 0;
        for (String url : productUrls) {
            if (count >= limit) break;
            try {
                System.out.println("Scraping Goldbox Deal (" + (count + 1) + "/" + limit + "): " + url);
                Deal deal = scrapeAmazonProduct(url);
                googleSheetService.saveDeal(deal);
                savedDeals.add(deal);
                count++;
            } catch (Exception e) {
                System.err.println("Error saving Goldbox deal [" + url + "]: " + e.getMessage());
            }
        }

        return savedDeals;
    }

    /**
     * Scrapes product title, price, and image URL from an Amazon product URL using Jsoup.
     */
    public Deal scrapeAmazonProduct(String amazonUrl) {
        Deal deal = new Deal();
        deal.setLink(amazonUrl);
        deal.setSource("Amazon");

        if (amazonUrl == null || amazonUrl.trim().isEmpty() || !amazonUrl.startsWith("http")) {
            System.err.println("Invalid Amazon URL provided: [" + amazonUrl + "]");
            deal.setTitle("Amazon Deal");
            deal.setPrice("N/A");
            deal.setImage("https://via.placeholder.com/500?text=Amazon+Deal");
            return deal;
        }

        try {
            Connection connection = Jsoup.connect(amazonUrl)
                    .userAgent(USER_AGENT)
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .referrer("https://www.google.com")
                    .timeout(10000)
                    .followRedirects(true);

            Document doc = connection.get();

            // 1. Extract Title
            String title = extractTitle(doc);
            deal.setTitle(title);

            // 2. Extract Price
            String price = extractPrice(doc);
            deal.setPrice(price);

            // 3. Extract Image
            String image = extractImage(doc);
            deal.setImage(image);

        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            System.err.println("Error scraping Amazon URL [" + amazonUrl + "]: " + errorMsg);
            // Fallback values if scraping blocked or network error
            if (deal.getTitle() == null || deal.getTitle().isEmpty()) {
                deal.setTitle("Amazon Deal - " + extractAsin(amazonUrl));
            }
            if (deal.getPrice() == null || deal.getPrice().isEmpty()) {
                deal.setPrice("N/A");
            }
            if (deal.getImage() == null || deal.getImage().isEmpty()) {
                deal.setImage("https://via.placeholder.com/500?text=Amazon+Deal");
            }
        }

        return deal;
    }

    /**
     * Extracts URL from raw text, HTML iframe, or SiteStripe link snippet.
     */
    public String extractUrl(String text) {
        if (text == null) return null;
        if (text.contains("href=\"")) {
            Matcher m = Pattern.compile("href=\"([^\"]+)\"").matcher(text);
            if (m.find()) return m.group(1);
        }
        if (text.contains("src=\"")) {
            Matcher m = Pattern.compile("src=\"([^\"]+)\"").matcher(text);
            if (m.find()) return m.group(1);
        }
        Matcher matcher = Pattern.compile("https?://[\\w\\.\\-\\?\\=/\\&\\%\\+]+").matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return text.trim();
    }

    /**
     * Follows HTTP redirects for short URLs like amzn.to
     */
    public String expandUrl(String shortUrl) {
        try {
            URL url = new URL(shortUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int responseCode = conn.getResponseCode();

            if (responseCode >= 300 && responseCode < 400) {
                String location = conn.getHeaderField("Location");
                if (location != null && !location.isEmpty()) {
                    return expandUrl(location);
                }
            }
            return shortUrl;
        } catch (Exception e) {
            return shortUrl;
        }
    }

    private String extractAsin(String url) {
        Matcher matcher = ASIN_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "B000000000";
    }

    private String extractTitle(Document doc) {
        Element titleEl = doc.getElementById("productTitle");
        if (titleEl != null) {
            return titleEl.text().trim();
        }

        Element metaTitle = doc.selectFirst("meta[name=title]");
        if (metaTitle != null && metaTitle.hasAttr("content")) {
            return metaTitle.attr("content").trim();
        }

        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        if (ogTitle != null && ogTitle.hasAttr("content")) {
            return ogTitle.attr("content").trim();
        }

        return doc.title().trim();
    }

    private String extractPrice(Document doc) {
        // Core Amazon price selectors
        String[] selectors = new String[]{
                "#corePrice_feature_div .a-price .a-offscreen",
                "#corePriceDisplay_desktop_feature_div .a-price .a-offscreen",
                "#priceblock_dealprice",
                "#priceblock_ourprice",
                ".a-price .a-offscreen",
                "span.apexPriceToPay .a-offscreen",
                "#kindle-price",
                ".priceToPay .a-offscreen"
        };

        for (String selector : selectors) {
            Element el = doc.selectFirst(selector);
            if (el != null && !el.text().trim().isEmpty()) {
                return cleanPrice(el.text());
            }
        }

        // Search for whole price + fraction
        Element whole = doc.selectFirst("span.a-price-whole");
        if (whole != null) {
            Element fraction = doc.selectFirst("span.a-price-fraction");
            String p = whole.text().replace(".", "").trim();
            if (fraction != null) {
                p += "." + fraction.text().trim();
            }
            return cleanPrice(p);
        }

        return "N/A";
    }

    private String cleanPrice(String priceRaw) {
        if (priceRaw == null) return "";
        // Remove currency symbols except numbers, dots, commas
        String cleaned = priceRaw.replaceAll("[^0-9.,]", "").trim();
        if (cleaned.endsWith(".")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned.isEmpty() ? priceRaw : cleaned;
    }

    private String extractImage(Document doc) {
        // High resolution image from landingImage
        Element mainImg = doc.getElementById("landingImage");
        if (mainImg != null) {
            if (mainImg.hasAttr("data-old-hires") && !mainImg.attr("data-old-hires").isEmpty()) {
                return mainImg.attr("data-old-hires");
            }
            if (mainImg.hasAttr("data-a-dynamic-image")) {
                String dynamicAttr = mainImg.attr("data-a-dynamic-image");
                // Extract first image URL from JSON key structure {"https://...":[w,h]}
                Matcher m = Pattern.compile("\"(https://[^\"]+)\"").matcher(dynamicAttr);
                if (m.find()) {
                    return m.group(1);
                }
            }
            if (mainImg.hasAttr("src")) {
                return mainImg.attr("src");
            }
        }

        Element ogImage = doc.selectFirst("meta[property=og:image]");
        if (ogImage != null && ogImage.hasAttr("content")) {
            return ogImage.attr("content");
        }

        Element imgBlk = doc.getElementById("imgBlkFront");
        if (imgBlk != null && imgBlk.hasAttr("src")) {
            return imgBlk.attr("src");
        }

        return "https://via.placeholder.com/500?text=Amazon+Product";
    }
}
