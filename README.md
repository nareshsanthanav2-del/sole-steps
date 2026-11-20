# sole

A minimalist, ultra-lightweight, open-source step and activity tracker for Android built with pure Kotlin, native Android Views, and custom Canvas rendering.

## Features

- **Pure OLED Monochrome UI**: Pitch-black (`#000000`) and crisp white (`#FFFFFF`) interface designed for clarity and minimal battery draw.
- **Ultra-Low Resource Footprint**: Uses Android's built-in low-power hardware step counter (`TYPE_STEP_COUNTER`), native SQLite, and zero third-party chart or networking libraries (~1.5 MB APK).
- **Today, Week & Trends Views**:
  - **Today**: Circular step progress ring, remaining step target, distance, calories, and active minutes.
  - **Week**: Interactive 7-day bar chart (Mon–Sun) with daily goal threshold line and per-day inspection.
  - **Trends**: Interactive 30-day smooth sparkline and monthly goal completion streak grid (Sun–Sat).
- **Optional AI Activity Logging**: Bring your own free Gemini API key to log custom activities (like cycling, swimming, or gym workouts) in plain language and add estimated metrics to your daily totals.
- **Flexible Tracking Modes**: Choose between zero-background-service hardware sensor reads on app open (default) or an optional lightweight background service.

## How to Set Up AI Activity Logging (API Key)

AI activity logging is completely optional and uses a "Bring Your Own Key" setup so you never need an account or subscription.

### Step 1: Get a Free Gemini API Key
1. Open **[Google AI Studio](https://aistudio.google.com/api-keys)** in your browser and sign in with your Google account.
2. Accept the terms of service if prompted.
3. Click **Create API key** in the top-right corner (if asked to select a project, choose the default project or create a new one).
4. Copy the generated API key to your clipboard. *(Treat your key like a password and never share it publicly.)*

### Step 2: Add the Key in sole
1. Open **sole** on your Android phone.
2. Tap your **profile circle** (showing your initial or photo) in the top-right corner of the **Today** screen to open **Settings**.
3. Scroll down to **AI activity logging** and make sure the switch is turned **ON**.
4. Paste your copied key into the **Google Cloud / Gemini API Key** box.
5. Tap **Save changes** at the bottom.

The **Log an activity** button will now appear at the bottom of your **Today** screen. Your API key stays stored locally on your device and is only sent directly to Google's Gemini API when you log an activity.

> **Prefer no AI?** Turn off **AI activity logging** in Settings (or leave the API key box blank) to completely hide all AI features from the app.

## How to Download & Install

1. Open the **[Releases](../../releases)** section on the right side of this GitHub page.
2. Download the latest `.apk` file onto your Android phone.
3. Open the downloaded file and allow installation from your browser/files app if prompted.

## License

MIT License — free for anyone to download, use, modify, and distribute.
