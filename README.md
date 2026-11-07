# Group Calendar App (P2P Telegram-Backed)

An Android Kotlin application providing decentralized group calendar availability synchronization using **Telegram Group Chats as a peer-to-peer data transport layer**.

---

## Key Architectural Principles

1. **Serverless / No Database Maintenance**:
   - The app does not need a private cloud database or hosting.
   - It treats a Telegram Group Chat as an append-only event stream.
2. **Conflict-Free File Ownership**:
   - Each group member exclusively manages their own availability JSON file: `user_{userId}.json`.
   - Edits never cause concurrency or write collisions.
3. **Jetpack Compose Availability Timeline**:
   - Dynamic Gantt-chart timeline mapping free and busy slots across all members.
   - Highlights mutual free windows where all group members are available.
4. **Offline Resilience & Background Synchronization**:
   - Profiles are cached locally on device storage.
   - Android `WorkManager` periodically polls Telegram in the background to ensure schedules stay updated.
5. **Direct APK Sideloading & Self-Updating**:
   - Pre-configured Gradle signing scheme for building release APKs (`./gradlew assembleRelease`).
   - Built-in In-App APK update downloader using Android `FileProvider` and `PackageInstaller`.

---

## Project Structure

```
Calendar-app/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/groupcalendar/app/
│       │   ├── GroupCalendarApp.kt
│       │   ├── data/
│       │   │   ├── api/          # Retrofit Telegram Bot Engine & OkHttpClient
│       │   │   ├── local/        # SharedPreferences & local profile cache
│       │   │   ├── model/        # MemberAvailability & Telegram DTOs
│       │   │   ├── repository/   # Single source of truth for UI state
│       │   │   └── sync/         # SyncManager & WorkManager background worker
│       │   ├── ui/
│       │   │   ├── components/   # Horizontal Availability Timeline & Gantt
│       │   │   ├── navigation/   # Navigation graph & routes
│       │   │   ├── screens/      # Home, EditSchedule, Settings, Onboarding
│       │   │   ├── theme/        # Material 3 colors, typography, theme
│       │   │   ├── viewmodel/    # CalendarViewModel
│       │   │   └── MainActivity.kt
│       │   └── util/             # DateTimeUtils, DeepLinkHelper, ApkUpdateHelper
│       └── res/
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── local.properties.example
└── README.md
```

---

## Quick Setup & Build

1. **Clone or open this folder in Android Studio (Hedgehog / Iguana / Jellyfish or newer)**.
2. Copy `local.properties.example` to `local.properties`:
   ```properties
   TELEGRAM_BOT_TOKEN=your_bot_token_here
   TELEGRAM_DEFAULT_CHAT_ID=-1001234567890
   TELEGRAM_GROUP_INVITE_LINK=https://t.me/+joinchat/...
   ```
3. To build a debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
4. To build an optimized release APK:
   ```bash
   ./gradlew assembleRelease
   ```
   The APK will be generated at:
   `app/build/outputs/apk/release/app-release.apk`
