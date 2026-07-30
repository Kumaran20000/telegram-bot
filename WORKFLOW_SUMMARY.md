# SiteStripe & Google Sheet Workflow Summary

## Overview
This document captures the current implementation status and workflow for automatically fetching product details from Amazon SiteStripe (or Amazon product links) and saving/updating them to Google Sheets, along with Telegram and Instagram automation.

---

## 🛠️ Created & Modified Components

### 1. **Amazon & SiteStripe Web Scraper Service**
- **File**: [`AmazonSiteStripeService.java`](file:///home/sudhakar/Kumaran/telegram-bot/src/main/java/com/example/telegram_bot/service/AmazonSiteStripeService.java)
- **Features**:
  - `processAndSaveSiteStripe(rawInput, customTitle, customPrice, customImage)`: Accepts Amazon URLs, shortlinks (`amzn.to`), or SiteStripe iframe/link snippets, automatically expands URLs, extracts missing product details (Title, Price, Image URL), and saves them to Google Sheet.
  - `scrapeAmazonProduct(amazonUrl)`: Jsoup-based HTML scraper extracting Amazon `#productTitle`, prices (`.a-price .a-offscreen`, `#priceblock_dealprice`), and high-res product images (`data-old-hires` / `data-a-dynamic-image`).
  - `enrichSheetDeals()`: Scans existing Google Sheet rows with missing details and populates them automatically from Amazon.
  - `scrapeGoldboxTopDeals(goldboxUrl, limit)`: Scrapes top N deal offers (e.g. 50 deals) from Amazon Goldbox / Today's Deals (`https://www.amazon.in/gp/goldbox`), extracts title, price, image URL, and product link, and appends them to Google Sheet.

### 2. **Google Sheet Service Updates**
- **File**: [`GoogleSheetService.java`](file:///home/sudhakar/Kumaran/telegram-bot/src/main/java/com/example/telegram_bot/service/GoogleSheetService.java)
- **Features**:
  - `saveDeal(Deal deal)`: Appends new deals with initial status `["NEW", "NEW"]` for Telegram and Instagram.
  - `updateDealRow(int rowNumber, Deal deal)`: Updates product details for specific rows in Google Sheet.
  - `getAllRows()`: Reads all spreadsheet rows for processing and enrichment.

### 3. **REST API Endpoints**
- **File**: [`DealController.java`](file:///home/sudhakar/Kumaran/telegram-bot/src/main/java/com/example/telegram_bot/controller/DealController.java)
- **Endpoints**:
  - `POST /api/deals/add-by-url`: Accepts JSON `{ "url": "https://..." }`, extracts details, and saves deal to Google Sheet.
  - `POST /api/deals/sitestripe`: Endpoint for Chrome Extension integration.
  - `GET /api/deals/preview-url?url=...`: Returns JSON preview of extracted details without writing to Google Sheet.
  - `POST /api/deals/enrich-sheet`: Triggers scan/enrichment of Google Sheet rows.
  - `POST /api/deals/goldbox?limit=50`: Scrapes top 50 offers from Amazon Goldbox (`https://www.amazon.in/gp/goldbox`) and adds them all to Google Sheet.
  - `GET /api/deals/grouped`: Returns all Google Sheet deals grouped into categories (e.g. Bluetooth / Audio, Watches, Laptops, Mobiles).
  - `GET /api/deals/carousel-by-category?category=bluetooth`: Returns grouped products and formatted carousel payload for the specified category.
  - `POST /api/deals/post-carousel?category=bluetooth`: Formats and posts a multi-slide Instagram Carousel post (`media_type=CAROUSEL` via Meta Graph API) for the given product category.

### 4. **Product Grouping & Instagram Carousel Service**
- **File**: [`CarouselService.java`](file:///home/sudhakar/Kumaran/telegram-bot/src/main/java/com/example/telegram_bot/service/CarouselService.java)
- **Features**:
  - `groupDealsByCategory()`: Groups all spreadsheet products into distinct categories (`HEADPHONE / Bluetooth`, `WATCH`, `LAPTOP`, `MOBILE`, `SHOE`, etc.).
  - `getDealsForCategoryName(query)`: Queries products matching specific category names (e.g. "bluetooth", "watch").
  - `postCategoryCarouselToInstagram(category)`: Formats and posts a multi-slide Instagram Carousel post via `InstagramService.publishInstagramCarousel(...)`.

### 5. **Chrome Extension**
- **Folder**: [`chrome-extension/`](file:///home/sudhakar/Kumaran/telegram-bot/chrome-extension/)
- **Files**:
  - `manifest.json`: Manifest V3 Chrome Extension definition.
  - `popup.html` & `popup.js`: Modern UI popup that captures the active Amazon tab, extracts product details (DOM + backend fallback), and saves to Google Sheet with 1-click.

---

## 🚀 How to Run & Resume

### A. Run Spring Boot Backend
```bash
./mvnw spring-boot:run
```

### B. Load Chrome Extension in Chrome
1. Open Google Chrome and navigate to `chrome://extensions`.
2. Enable **Developer mode** (toggle in top-right corner).
3. Click **Load unpacked** and select the folder:
   `/home/sudhakar/Kumaran/telegram-bot/chrome-extension`
4. Open any Amazon product page or copy a SiteStripe link, click the Extension icon, and click **Save to Google Sheet**!

### C. Testing REST Endpoints
```bash
# Add Amazon product by URL
curl -X POST http://localhost:8080/api/deals/add-by-url \
  -H "Content-Type: application/json" \
  -d '{"url":"https://www.amazon.in/dp/B08N5WRWNW"}'

# Enrich incomplete Google Sheet rows
curl -X POST http://localhost:8080/api/deals/enrich-sheet

# Scrape top 50 Goldbox deals from https://www.amazon.in/gp/goldbox and add to Google Sheet
curl -X POST "http://localhost:8080/api/deals/goldbox?limit=50"
```

---

## 📌 Next Steps when Restarting
1. Launch `./mvnw spring-boot:run` to test live Google Sheet appends and scraping.
2. Load the Chrome extension in Chrome to test 1-click SiteStripe deal creation.
3. Check Google Sheet for automated status updates when `PostScheduler` processes deals for Telegram and Instagram.
