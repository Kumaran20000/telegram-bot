# SiteStripe & Google Sheet Workflow Summary

## Overview
This document captures the current implementation status and complete workflow for automatically fetching, enriching, saving, and publishing Amazon affiliate product deals to Google Sheets, Telegram, and Instagram.

---

## 🛠️ Components & Developed Features

### 1. **Amazon & SiteStripe Web Scraper Service**
- **File**: [`AmazonSiteStripeService.java`](file:///home/sudhakar/Kumaran/telegram-bot/src/main/java/com/example/telegram_bot/service/AmazonSiteStripeService.java)
- **Features**:
  - `processAndSaveSiteStripe(rawInput, customTitle, customPrice, customImage)`: Accepts Amazon URLs, shortlinks (`amzn.to`), or SiteStripe iframe/link snippets, automatically expands URLs, extracts missing product details (Title, Price, Image URL), and saves them to Google Sheet.
  - `scrapeAmazonProduct(amazonUrl)`: Jsoup-based HTML scraper extracting Amazon `#productTitle`, prices (`.a-price .a-offscreen`, `#priceblock_dealprice`), and high-res product images (`data-old-hires` / `data-a-dynamic-image`).
  - `enrichSheetDeals()`: Scans Google Sheet for raw links pasted in Column A (Title) or Column D (Link), auto-expands URLs, scrapes missing product details from Amazon, and updates the Google Sheet row.
  - `scrapeGoldboxTopDeals(goldboxUrl, limit)`: Scrapes top N deal offers (e.g. 10 deals) from Amazon Goldbox / Today's Deals (`https://www.amazon.in/gp/goldbox`), attaches your Amazon Associate tag, and appends unique non-duplicate deals to Google Sheet.
  - `attachAffiliateTag(url)`: Automatically stitches or replaces your Amazon Associate store ID tag (`amazon.associate.tag`) on product URLs so 100% of deals earn affiliate commissions.

### 2. **Google Sheet Service Updates**
- **File**: [`GoogleSheetService.java`](file:///home/sudhakar/Kumaran/telegram-bot/src/main/java/com/example/telegram_bot/service/GoogleSheetService.java)
- **Features**:
  - `saveDeal(Deal deal)`: Appends new deals with initial status `["NEW", "NEW"]` for Telegram and Instagram.
  - `updateDealRow(int rowNumber, Deal deal)`: Updates product details for specific rows in Google Sheet.
  - `getAllRows()`: Reads all spreadsheet rows for processing and enrichment.
  - **Incomplete Deal Validation & Skipping**: Validates required fields (`title`, `price`, `image`, `link`). Skips incomplete/invalid rows (`N/A`, empty) with clear console warnings.

### 3. **Automated Schedulers**
- **File**: [`PostScheduler.java`](file:///home/sudhakar/Kumaran/telegram-bot/src/main/java/com/example/telegram_bot/scheduler/PostScheduler.java)
  - **5-Minute Deal Cycle**: Runs every 5 minutes (`fixedRate = 300000`).
  - **Auto-Enrichment Integration**: Automatically runs `enrichSheetDeals()` before processing deal posting.
  - **Network Resilience**: Catches `java.net.UnknownHostException` / DNS errors gracefully without thread crashes and sends an admin notification.
- **File**: [`DailyDealFetcherScheduler.java`](file:///home/sudhakar/Kumaran/telegram-bot/src/main/java/com/example/telegram_bot/scheduler/DailyDealFetcherScheduler.java)
  - **Automated Daily Products**: Scrapes and appends **50 top products daily** from Amazon Today's Deals at 09:35 AM local time (`0 35 09 * * ?`).
  - **Configurable**: Configurable via `daily.deal.fetch.limit=50`, `daily.deal.fetch.cron`, and `daily.deal.fetch.zone` in `application.properties`.
- **File**: [`SocialMediaScheduleScheduler.java`](file:///home/sudhakar/Kumaran/telegram-bot/src/main/java/com/example/telegram_bot/scheduler/SocialMediaScheduleScheduler.java)
  - **Automated Daily Facebook & Instagram Schedule**:
    - **2 Offer Reels / Day**: Published at 11:30 AM IST and 07:30 PM IST on Facebook & Instagram.
    - **1 Carousel Post / Day**: Published at 03:00 PM IST on Facebook & Instagram.
    - **4 Stories / Day**: Published at 09:00 AM, 01:30 PM, 05:30 PM, and 09:30 PM IST on Facebook & Instagram.
  - **Configurable Crons**: Configurable via `social.schedule.*` properties in `application.properties`.


### 4. **Dynamic Deal Score & Intelligent Ranking Engine**
- **File**: [`Deal.java`](file:///home/sudhakar/Kumaran/telegram-bot/src/main/java/com/example/telegram_bot/model/Deal.java) & [`DealScoreService.java`](file:///home/sudhakar/Kumaran/telegram-bot/src/main/java/com/example/telegram_bot/service/DealScoreService.java)
  - **Dynamic Scoring Formula**:
    $$\text{Deal Score} = \text{Discount \%} + \text{Price Attractiveness} + \text{Product Popularity} + \text{Category Demand} + \text{Previous Performance Score}$$
  - **Score Breakdown**:
    - **Discount %** (0 to 100 pts): Savings percentage from MRP.
    - **Price Attractiveness** (0 to 30 pts): Higher score for impulse price points (< ₹500 = +30, < ₹1500 = +25, etc.).
    - **Product Popularity** (0 to 25 pts): Brand detection (Apple, Samsung, Sony, Boat, Noise, Nike, Dell, etc.).
    - **Category Demand** (0 to 25 pts): High conversion categories (Headphones/Mobiles = 25, Smartwatches/Laptops = 20).
    - **Previous Performance Score** (0 to 20 pts): Historical performance weightage & freshness bonus.
  - **Automated Prioritization**: Schedulers rank available deals by `Deal Score` and select top-scoring deals first for Reels, Carousels, and Stories.

### 5. **Enhanced Post Formatting & Deal Rating Engine**
  - `calculateDiscountPercent()`: Calculates exact savings % from MRP vs Deal Price.
  - `getDealRatingBadge()`: Dynamically assigns Deal Rating Badges (e.g. `🔥 SUPER DEAL (65% OFF) 🌟🌟🌟🌟🌟`, `⚡ HOT DEAL (45% OFF) ⭐⭐⭐⭐`).
- **File**: [`MessageFormatterService.java`](file:///home/sudhakar/Kumaran/telegram-bot/src/main/java/com/example/telegram_bot/service/MessageFormatterService.java)
  - **Category Emojis**: Auto-detects product category and applies matching visual badges (🎧 Audio, 📱 Mobiles, ⌚ Watches, 💻 Laptops, 👟 Shoes, 🏠 Home).
  - **HTML Formatting**: Formats bold titles, Price in ₹, struck-through `<s>MRP: ₹2,499</s>`, discount % badges, store badges (`🏷️ Store: Amazon`), and limited-time offer warnings.
- **File**: [`TelegramService.java`](file:///home/sudhakar/Kumaran/telegram-bot/src/main/java/com/example/telegram_bot/service/TelegramService.java)
  - `sendPhotoWithButton(...)`: Sends high-resolution product photos with formatted HTML captions and interactive **Inline Keyboard Action Buttons** (`🛒 Buy Now on Amazon`).
  - `sendMessageWithButton(...)`: HTML text post fallback with interactive buttons.
  - `sendAdminNotification(...)`: Real-time admin Telegram alert engine.
- **File**: [`InstagramService.java`](file:///home/sudhakar/Kumaran/telegram-bot/src/main/java/com/example/telegram_bot/service/InstagramService.java) & [`CaptionService.java`](file:///home/sudhakar/Kumaran/telegram-bot/src/main/java/com/example/telegram_bot/service/CaptionService.java)
  - Video Reel (`/video/stream`) and 1:1 image card generation (`/video/image/stream`).
  - Multi-step publication fallback (Reel -> 1:1 Image -> Direct Public URL).
  - Category-tailored hashtag generation (`#amazonfinds #offer #discount ...`).

### 5. **Admin Telegram Notifications & Alert System**
- **Config**: `telegram.admin.chat.id` in `application.properties`.
- **Alert Types**:
  - **Daily Fetch Summaries**: Notification when the 10 daily deals are added to Google Sheet.
  - **Posting Failure Alerts**: Instant notification when a deal fails to publish to Telegram or Instagram.
  - **Network / Connectivity Warnings**: Instant alert on DNS / Google OAuth API connection errors.

### 6. **REST API Endpoints**
- **File**: [`DealController.java`](file:///home/sudhakar/Kumaran/telegram-bot/src/main/java/com/example/telegram_bot/controller/DealController.java)
- **Endpoints**:
  - `POST /api/deals/add-by-url`: Accepts JSON `{ "url": "https://..." }`, extracts details, and saves deal to Google Sheet.
  - `POST /api/deals/sitestripe`: Endpoint for Chrome Extension integration.
  - `GET /api/deals/preview-url?url=...`: Returns JSON preview of extracted details without writing to Google Sheet.
  - `POST /api/deals/enrich-sheet`: Triggers scan/enrichment of Google Sheet rows.
  - `POST /api/deals/daily-fetch?limit=10`: Triggers daily deal fetch on demand (adds 10 products).
  - `POST /api/deals/goldbox?limit=50`: Scrapes top N offers from Amazon Goldbox (`https://www.amazon.in/gp/goldbox`) and adds them all to Google Sheet.
  - `GET /api/deals/grouped`: Returns all Google Sheet deals grouped into categories.
  - `GET /api/deals/carousel-by-category?category=bluetooth`: Returns grouped products and formatted carousel payload.
  - `POST /api/deals/post-carousel?category=bluetooth`: Formats and posts a multi-slide Instagram Carousel post (`media_type=CAROUSEL` via Meta Graph API).
  - `POST /api/deals/post-facebook`: Test endpoint to publish a deal to a Facebook Page on demand.
  - `GET /api/deals/facebook-token-status`: Checks whether the configured Facebook Page Access Token is valid or expired.
  - `GET /api/deals/schedule-status`: Checks live daily social schedule metrics (Reels: 2/day, Carousel: 1/day, Stories: 4/day).
  - `POST /api/deals/post-reel`: Triggers an Offer Reel publication to Facebook & Instagram on demand.
  - `GET /api/deals/youtube-shorts-package/top`: Automatically picks top-scored deal, generates 1080x1920 video, and returns full YouTube Shorts upload package.
  - `GET /api/deals/youtube-shorts-format`: Formats YouTube Shorts Title, Description, Tags, and Pinned Comment.
  - `GET /api/deals/whatsapp-format`: Formats deal message using native WhatsApp markdown (`*bold*`, `~strike~`, direct buy link, and Telegram invite).
  - `GET /api/deals/growth-blueprint`: Returns the multi-platform growth blueprint, profile templates, and posting rules.
  - `GET /api/deals/trending-audio`: Lists catalog of upbeat (120–135 BPM) Instagram Reels trending tracks.
  - `GET /api/deals/trending-audio/recommend?title=...`: Returns recommended trending track, BPM, and Instagram Reels library search query.
  - `POST /api/deals/schedule/trigger-reel`: Manually triggers an instant Offer Reel publication.
  - `POST /api/deals/schedule/trigger-carousel`: Manually triggers an instant Category Carousel publication.
  - `POST /api/deals/instagram-sheet/post-carousel?group=...`: Publishes a clean non-HTML grouped carousel from the dedicated Instagram sheet tab.
  - `POST /api/deals/instagram-sheet/post-reel`: Publishes the next pending reel from the dedicated Instagram sheet tab.
  - `GET /api/deals/ranked`: Returns all available deals in Google Sheet ranked by Deal Score with score breakdown.

*For complete multi-platform publishing and growth strategies, see [`MULTI_PLATFORM_GROWTH_WORKFLOW.md`](file:///home/sudhakar/Kumaran/telegram-bot/MULTI_PLATFORM_GROWTH_WORKFLOW.md).*



### 7. **Chrome Extension**
- **Folder**: [`chrome-extension/`](file:///home/sudhakar/Kumaran/telegram-bot/chrome-extension/)
- **Files**:
  - `manifest.json`: Manifest V3 Chrome Extension definition.
  - `popup.html` & `popup.js`: Modern UI popup that captures active Amazon tab, extracts product details, and saves to Google Sheet with 1-click.

---

## 🔑 Resolving Facebook/Meta Token Expiration (`OAuthException 190, subcode 463`)

When Meta Access Tokens expire, Facebook and Instagram Graph API requests fail. Follow these steps to generate a **Never-Expiring Page Access Token**:

1. **Open Meta Graph API Explorer**:
   - Go to [developers.facebook.com/tools/explorer/](https://developers.facebook.com/tools/explorer/)
   - Select your Meta App and add permissions: `pages_show_list`, `pages_read_engagement`, `pages_manage_posts`.
2. **Generate User Token & Select Page**:
   - Click **Get User Access Token** and approve the prompt.
   - Under **User or Page**, select your Facebook Page to generate a **Page Access Token**.
3. **Exchange for Long-Lived User Token (60 days)**:
   ```http
   GET https://graph.facebook.com/v23.0/oauth/access_token?
     grant_type=fb_exchange_token&
     client_id={YOUR_APP_ID}&
     client_secret={YOUR_APP_SECRET}&
     fb_exchange_token={SHORT_LIVED_USER_TOKEN}
   ```
4. **Fetch Never-Expiring Page Access Token**:
   - Query Meta Graph API using your long-lived user token:
   ```http
   GET https://graph.facebook.com/v23.0/{YOUR_PAGE_ID}?fields=access_token&access_token={LONG_LIVED_USER_TOKEN}
   ```
   - The returned `access_token` for your page **will never expire**!
5. **Update Environment Variable**:
   - Update `FACEBOOK_PAGE_ACCESS_TOKEN` on **Render.com** (or in `application.properties`).
   - Verify token health via: `GET http://localhost:8080/api/deals/facebook-token-status`.

---

## ⚙️ Configuration File (`application.properties`)

```properties
spring.application.name=telegram-bot
telegram.bot.token=8247976420:AAHW9Wu_EFwEP_xcHSAhti_rGfDlNJSdwvo
telegram.chat.id=@BOnlinediscount
telegram.admin.chat.id=@BOnlinediscount

google.sheet.id=1z3U5J2qDkRhXQ8muPtPazF0a_bSiX7fYVDO9uVUSlM0
google.sheet.range=Sheet1!A2:H

instagram.access-token=EAAPYjux20jkBSHLuEZBvw8ZBDi4no7OqTqRp0Cc88ZAycMLNPt1B3dNHaWmVvHQGJX6aCdyGHq8NpizZAvWSDifsVfLZANQs0NG823JSZB7Sj84cbxpyYk8YUU1TCdCWUANbHvOwoZC9wU8HjifMDtcNJJYwC3RJjMGU2rFMFvimafCotCFEZA6uQxviDwZAP
instagram.business-id=17841409837583820

# Facebook Page Configuration
facebook.page.access-token=EAAPYjux20jkBS...
facebook.page.id=4088491754782444
facebook.enabled=true

app.server.base-url=http://localhost:8080

# Daily Deal Fetcher Configuration (Default: 10 deals daily at 11:10 AM IST)
daily.deal.fetch.enabled=true
daily.deal.fetch.limit=10
daily.deal.fetch.cron=0 10 11 * * ?
daily.deal.fetch.zone=Asia/Kolkata

# Amazon Associate Affiliate Tag (e.g. offerzone21-21)
amazon.associate.tag=dealszone0a9-21
```

---

## 🚀 How to Run & Test

### A. Run Spring Boot Backend
```bash
./mvnw spring-boot:run
```

### B. Testing Key Features via REST Endpoints
```bash
# 1. Check Facebook Page Access Token Status
curl -X GET "http://localhost:8080/api/deals/facebook-token-status"

# 2. Manually trigger 10 Daily Products Fetch to Google Sheet
curl -X POST "http://localhost:8080/api/deals/daily-fetch?limit=10"

# 3. Enrich incomplete Google Sheet rows (scrape missing price/title/image)
curl -X POST "http://localhost:8080/api/deals/enrich-sheet"

# 4. Add Amazon product by URL
curl -X POST http://localhost:8080/api/deals/add-by-url \
  -H "Content-Type: application/json" \
  -d '{"url":"https://www.amazon.in/dp/B08N5WRWNW"}'

# 5. Post multi-slide Instagram carousel by category
curl -X POST "http://localhost:8080/api/deals/post-carousel?category=bluetooth"
```

### C. Load Chrome Extension in Chrome
1. Open Google Chrome and navigate to `chrome://extensions`.
2. Enable **Developer mode** (toggle in top-right corner).
3. Click **Load unpacked** and select the folder:
   `/home/sudhakar/Kumaran/telegram-bot/chrome-extension`
4. Open any Amazon product page or copy a SiteStripe link, click the Extension icon, and click **Save to Google Sheet**!
