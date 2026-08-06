# 🚀 Free Deployment Guide: Render.com

This repository is fully configured for deployment on **Render.com** using Docker.

---

## 📋 Prerequisites
1. A **GitHub** account with this repository pushed to it.
2. A free account on **[Render.com](https://render.com)**.

---

## ⚡ Deployment Steps (1-Click or Manual)

### Step 1: Push Code to GitHub
Make sure all recent changes are pushed to your GitHub repository:
```bash
git add .
git commit -m "Configure Docker & application.properties for Render deployment"
git push origin main
```

---

### Step 2: Create a New Web Service on Render

1. Log in to your [Render Dashboard](https://dashboard.render.com).
2. Click **New +** → **Web Service**.
3. Connect your **GitHub repository** (`telegram-bot`).
4. Set the following configuration parameters:
   - **Name**: `telegram-bot` *(or your preferred name)*
   - **Region**: Select closest region (e.g. *Singapore* or *Oregon*)
   - **Language / Runtime**: **Docker**
   - **Instance Type**: **Free** (512 MB RAM, 0.1 CPU)

---

### Step 3: Add Environment Variables on Render

In the **Environment Variables** section on Render, add the following variables:

| Key | Value / Description | Example |
| :--- | :--- | :--- |
| `APP_SERVER_BASE_URL` | Your Render app HTTPS URL | `https://telegram-bot-xxxx.onrender.com` |
| `TELEGRAM_BOT_TOKEN` | Telegram Bot API token from `@BotFather` | `8247976420:AAHW9...` |
| `TELEGRAM_CHAT_ID` | Main Telegram Channel | `@BOnlinediscount` |
| `TELEGRAM_ADMIN_CHAT_ID` | Private Admin Telegram Chat ID | *(Optional)* |
| `INSTAGRAM_ACCESS_TOKEN` | Meta Graph API Access Token | `EAAPYjux20jk...` |
| `INSTAGRAM_BUSINESS_ID` | Meta Instagram Business Account ID | `17841409837583820` |
| `GOOGLE_SHEET_ID` | Google Sheet ID | `1z3U5J2qDkRh...` |
| `GOOGLE_CREDENTIALS_JSON` | Content of your Google Service Account `credentials.json` | `{"type": "service_account", ...}` |
| `AMAZON_ASSOCIATE_TAG` | Amazon Associate Store Tag | `dealszone0a9-21` |

> 💡 **Tip for GOOGLE_CREDENTIALS_JSON**: Copy and paste the entire raw contents of your local `credentials.json` file as a single line into the `GOOGLE_CREDENTIALS_JSON` variable.

---

### Step 4: Click Deploy Web Service!

1. Click **Deploy Web Service**.
2. Render will build the Docker container using Maven + Java 17 and start your Spring Boot application.
3. Once deployed, note down your live URL (e.g., `https://telegram-bot-xxxx.onrender.com`).
4. Update `APP_SERVER_BASE_URL` in the Render Environment Variables tab to match `https://telegram-bot-xxxx.onrender.com`.

---

## ⏰ Keeping Render Awake (Prevent Sleeping)

Render free Web Services go to sleep after 15 minutes of HTTP inactivity.

Although your `PostScheduler` runs every 5 minutes, to ensure 100% continuous uptime without sleeping:
1. Go to **[UptimeRobot](https://uptimerobot.com/)** (Free account).
2. Add a new **HTTP Monitor**:
   - **URL**: `https://telegram-bot-xxxx.onrender.com/api/deals/grouped`
   - **Interval**: Every 5 or 10 minutes.
3. This keeps your bot active 24/7 without extra costs.

---

## 🛠️ Verification & Endpoints

Once live, verify your service by hitting:
* `GET https://telegram-bot-xxxx.onrender.com/api/deals/grouped` (Returns deals list)
* `POST https://telegram-bot-xxxx.onrender.com/api/deals/daily-fetch?limit=10` (Triggers deal fetch)
