# sole

A minimalist, ultra-lightweight, open-source step and activity tracker for Android built with pure Kotlin, native Android Views, and custom Canvas rendering.

## Features

- **Pure OLED Monochrome UI**: Pitch-black (`#000000`) and crisp white (`#FFFFFF`) interface designed for clarity and minimal battery draw.
- **Ultra-Low Resource Footprint**: Uses Android's built-in low-power hardware step counter (`TYPE_STEP_COUNTER`), native SQLite, and zero third-party chart or networking libraries (~1.5 MB APK).
- **Today, Week & Trends Views**:
  - **Today**: Circular step progress ring, remaining step target, distance, calories, and active minutes.
  - **Week**: Interactive 7-day bar chart (Mon–Sun) with daily goal threshold line and per-day inspection.
  - **Trends**: Interactive 30-day smooth sparkline and monthly goal completion streak grid (Sun–Sat).
- **Optional AI Activity Logging**: Add your own Google Cloud / Gemini API key in Settings to log any custom activity in natural language, preview estimated metrics, and confirm before adding them to your daily totals. Disable AI in Settings to remove all AI UI completely.
- **Flexible Tracking Modes**: Choose between zero-background-service hardware sensor reads on app open (default) or an optional lightweight background service.

## How to Build the APK on GitHub (No Coding Setup Needed)

1. Create a new public repository on GitHub named `sole`.
2. Upload all files from this project into the repository.
3. Open the **Actions** tab in your GitHub repository — GitHub will automatically compile the Android `.apk` file.
4. Once the build completes (green checkmark), click the workflow run and download **`sole-apk`** under **Artifacts** (or publish it under **Releases** so anyone can download it with one click).

## License

MIT License — free for anyone to download, use, modify, and distribute.
