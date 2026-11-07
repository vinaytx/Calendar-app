# Group Calendar App: Architecture, Production Deployment & Operations Manual

> **A Serverless, Decentralized Group Availability Synchronization System Backed by Telegram Infrastructure**  
> **Target OS:** Android 8.0+ (API 26 – 34)  
> **UI Architecture:** Jetpack Compose (Material 3)  
> **Data Transport:** Telegram Bot API (P2P Event Streaming)  
> **Version:** 1.0.0

---

## Executive Summary

The **Group Calendar App** provides peer-to-peer (P2P) calendar availability synchronization across teams, friend groups, and families without requiring dedicated central database servers (e.g. Firebase, PostgreSQL, AWS RDS). 

Instead, the app uses a **Telegram Group Chat as a decentralized append-only event stream and distributed file store**. Each member exclusively owns and broadcasts their own JSON availability file (`user_{userId}.json`), eliminating concurrency conflicts while giving users 100% control over their calendar data.

---

## 1. System Architecture & Peer-to-Peer Data Flow

```
[Alice's Phone] ────(Generates user_98765.json)────► [Telegram Group Chat]
                                                               │
                                  ┌────────────────────────────┴────────────────────────────┐
                                  ▼ (Polls updates via Bot API)                             ▼
                           [Bob's Phone]                                             [Charlie's Phone]
                                  │                                                         │
                                  ▼                                                         ▼
                     [Reconciles Local Map & UI]                               [Reconciles Local Map & UI]
```

### Key Workflow Mechanisms:
1. **Single-Writer File Ownership**: To completely eliminate race conditions, user conflicts, and database locking, **each group member uniquely owns and writes to their own availability file** (`user_{userId}.json`). No member can overwrite another member's availability file.
2. **State Updates & Broadcast**: Whenever a user adds, modifies, or deletes a busy interval, the local client serializes a fresh JSON payload with an updated UTC ISO-8601 timestamp and uploads it to the Telegram Group Chat via `sendDocument`.
3. **Log Ingestion & Reconciliation**: Group members fetch chat updates (`getUpdates`), filter message history for documents matching `user_*.json`, download newer payloads, and merge them into their local repository.
4. **Offline Resilience**: Payloads are stored in local device storage (`filesDir/member_profiles/`). If a user is offline, the app displays the last known roster state.

---

## 2. File Data Structure (`MemberAvailability.json`)

Each group member maintains a file adhering strictly to this schema:

```json
{
  "userId": "user_98765",
  "userName": "Alice Smith",
  "lastUpdated": "2026-11-07T05:30:00Z",
  "colorHex": "#3B82F6",
  "busySlots": [
    {
      "start": "2026-11-08T09:00:00",
      "end": "2026-11-08T11:00:00",
      "title": "Strategy Sync"
    },
    {
      "start": "2026-11-08T14:30:00",
      "end": "2026-11-08T16:00:00",
      "title": "Client Review"
    }
  ]
}
```

### Field Definitions:
- **`userId`** *(String, Required)*: Unique client identifier (e.g., `user_a1b2c3d4`).
- **`userName`** *(String, Required)*: Human-readable display name shown on timeline tracks.
- **`lastUpdated`** *(String, Required)*: UTC ISO-8601 timestamp for chronological reconciliation.
- **`colorHex`** *(String, Optional)*: Hexadecimal accent color for personalized timeline avatars.
- **`busySlots`** *(Array, Required)*: Occupied time blocks. Hours outside these slots are considered free.
- **`busySlots.start` / `busySlots.end`** *(String, Required)*: ISO-8601 start and end timestamps.
- **`busySlots.title`** *(String, Optional)*: Label shown when a busy block is inspected.

---

## 3. Implementation Codebase Structure

The application is fully generated and organized under `C:\Shared\dev\Calendar-app`:

```
Calendar-app/
├── app/
│   ├── build.gradle.kts             # Dependencies, BuildConfig, and SigningConfigs
│   ├── proguard-rules.pro           # Serialization and Retrofit keep rules
│   └── src/main/
│       ├── AndroidManifest.xml      # Permissions, FileProvider, Deep Link Intent Filters
│       ├── java/com/groupcalendar/app/
│       │   ├── GroupCalendarApp.kt  # Application entrypoint & WorkManager scheduler
│       │   ├── data/
│       │   │   ├── api/
│       │   │   │   ├── TelegramCalendarEngine.kt  # Retrofit API interface
│       │   │   │   └── RetrofitClient.kt          # OkHttpClient with logging & timeouts
│       │   │   ├── local/
│       │   │   │   └── PreferencesManager.kt      # Encrypted/Private SharedPreferences & cache
│       │   │   ├── model/
│       │   │   │   └── MemberAvailability.kt      # Domain models & Telegram DTOs
│       │   │   ├── repository/
│       │   │   │   └── CalendarRepository.kt      # Repository & StateFlow streams
│       │   │   └── sync/
│       │   │       ├── GroupCalendarSyncManager.kt# Broadcast & Fetch sync logic
│       │   │       └── CalendarSyncWorker.kt      # WorkManager periodic background worker
│       │   ├── ui/
│       │   │   ├── components/
│       │   │   │   └── AvailabilityTimeline.kt    # Dynamic Jetpack Compose Gantt tracks
│       │   │   ├── navigation/
│       │   │   │   └── Screen.kt                  # Sealed routes & navigation hierarchy
│       │   │   ├── screens/
│       │   │   │   ├── HomeScreen.kt              # Group roster & All Free highlights
│       │   │   │   ├── EditScheduleScreen.kt      # Time slot editor & broadcast CTA
│       │   │   │   ├── SettingsScreen.kt          # Bot token, group ID, APK update
│       │   │   │   └── OnboardingScreen.kt        # 3-step setup wizard
│       │   │   ├── theme/                         # Material 3 colors, typography, theme
│       │   │   ├── viewmodel/
│       │   │   │   └── CalendarViewModel.kt       # ViewModel managing app state
│       │   │   └── MainActivity.kt                # Jetpack Compose activity host
│       │   └── util/
│       │       ├── DateTimeUtils.kt               # Free slot intersection algorithm
│       │       ├── TelegramDeepLinkHelper.kt      # tg:// and https://t.me/ intent launcher
│       │       └── ApkUpdateHelper.kt             # In-app APK self-update downloader
│       └── res/
├── gradle/
│   ├── libs.versions.toml           # Version Catalog
│   └── wrapper/
├── build.gradle.kts                 # Root Gradle build script
├── settings.gradle.kts              # Module inclusion
├── gradle.properties                # JVM & AndroidX flags
├── local.properties.example         # Template for Bot Token & Keystore passwords
├── README.md
├── generate_documentation.ps1       # Word Document (.docx) compiler script
└── Group_Calendar_App_Design_Deployment_Usage.docx # Generated Word Document
```

---

## 4. Jetpack Compose Availability Timeline Component

The UI features a dynamic horizontal Gantt timeline (`AvailabilityTimeline.kt`):
- **Hour Ruler**: Spans 08:00 to 22:00 (14-hour daily window).
- **"👥 Everyone Free" Track**: Dynamically runs the `DateTimeUtils.findCommonFreeSlots()` algorithm across all group members and paints green blocks where *every single member* is available.
- **Member Tracks**: Displays each member's avatar, initials, and occupied hours color-coded in red with event labels. Tapping any slot opens detailed start/end times.

---

## 5. Security Isolation Architecture

1. **Compile-Time Isolation (`local.properties`)**:
   - Secrets (`TELEGRAM_BOT_TOKEN`, `TELEGRAM_DEFAULT_CHAT_ID`, `TELEGRAM_GROUP_INVITE_LINK`) reside in `local.properties`.
   - Injected into `BuildConfig` at compile time and never committed to Git.
2. **Runtime Dynamic Configuration**:
   - If compiled without default credentials, users configure their Bot Token and Group ID via the in-app **Settings** or **Onboarding** wizard.
3. **Multi-Tenant Readiness**:
   - The same APK binary can be used across completely separate teams, friend circles, or family groups simply by pointing to different Telegram bots and chats.

---

## 6. APK Build, Cryptographic Signing & Sideload Distribution

### 1. Generating a Release Keystore
```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-app-key
```

### 2. Configure `local.properties`
```properties
RELEASE_KEYSTORE_PATH=my-release-key.jks
RELEASE_KEYSTORE_PASSWORD=your_keystore_password
RELEASE_KEY_ALIAS=my-app-key
RELEASE_KEY_PASSWORD=your_key_password
```

### 3. Build Command
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

### 4. Distribution Channels
- **Direct Telegram Share**: Drag and drop `app-release.apk` directly into the Telegram group.
- **GitHub Releases**: Tag releases (e.g. `v1.0.0`) and attach the APK.
- **Obtainium / F-Droid**: Users can track updates automatically directly from GitHub releases.

---

## 7. Overcoming Android OS Sideloading Barriers

When users install an APK directly, Android displays two standard security prompts:

### Barrier 1: "Install Unknown Apps"
- **Prompt**: *"For your security, your phone is not allowed to install unknown apps from this source."*
- **Resolution**: Tap **Settings** ➔ Toggle **"Allow from this source"** ➔ Tap **Back** ➔ Tap **Install**.

### Barrier 2: Google Play Protect Warning
- **Prompt**: *"Blocked by Play Protect" / "Unrecognized app"*.
- **Resolution**: Tap **"More details"** (small expand arrow) ➔ Tap **"Install anyway"**.

---

## 8. In-App Self-Updating Engine

The app contains built-in self-updating via `ApkUpdateHelper.kt`:
1. Checks for newer APK builds via remote URL.
2. Downloads the binary to `cacheDir/update_app.apk` with progress tracking.
3. Obtains a secure URI via Android `FileProvider`.
4. Dispatches `Intent.ACTION_VIEW` to the system `PackageInstaller` for an in-place upgrade without data loss.

---

## 9. End-to-End Setup, Deployment & User Operations Guide

### Step 1: Create the Telegram Bot
1. Open Telegram and message **[@BotFather](https://t.me/BotFather)**.
2. Send `/newbot` and follow prompts to set a name and username (e.g. `AlphaTeamCalendar_bot`).
3. Copy the **HTTP API Token** (e.g. `123456789:ABCdefGHIjklMNOpqrSTUvwxYZ`).
4. **CRITICAL**: Send `/setprivacy` to `@BotFather`, select your bot, and choose **`DISABLED`**. This allows the bot to read document uploads in group chats.

### Step 2: Create Group & Add Bot
1. Create a new Telegram group (e.g. "Alpha Team").
2. Add your bot to the group and promote it to **Administrator** with *Send Messages* and *Send Files* privileges.
3. Generate a **Group Invite Link** (`https://t.me/+...`).

### Step 3: Find Group Chat ID
1. In your browser, open:  
   `https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates`
2. Post any message in the Telegram group chat.
3. Refresh the browser and find `"chat":{"id": -1001234567890, ...}`. The negative number is your **Chat ID**.

### Step 4: Distribute & Onboard
1. Share `app-release.apk` and the Group Invite Link with your members.
2. Users open the app and complete the 3-step setup:
   - Enter Display Name & choose avatar accent color.
   - Tap to join group via deep link.
   - Confirm connection.
3. Users tap **Update My Schedule** ➔ **Add Busy Slot** ➔ **Broadcast**. The entire group timeline updates in real time!

---

## 10. Troubleshooting & FAQ

| Problem | Root Cause | Solution |
| :--- | :--- | :--- |
| **HTTP 403 Forbidden** | Bot is not an admin or lacks file send permissions | Add bot to group and grant Administrator status with file permissions. |
| **getUpdates returns empty** | Telegram Privacy Mode active | Send `/setprivacy` to `@BotFather` and set to `DISABLED`. |
| **Sync stops when phone sleeps** | Android Battery Optimization | Set app battery profile to *Unrestricted* in Android Settings. |
| **Play Protect warning** | App not indexed in Play Store | Tap *More details* ➔ *Install anyway*. |
| **Install fails over existing** | Keystore mismatch | Uninstall previous debug build before installing release-signed APK. |
