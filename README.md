<div align="center">

# <img src="docs/logo.svg" width="48" height="48" align="center"> SentinelShield

### Advanced Open-Source Android Anti-Theft & Device Protection Suite

[![Android Min SDK](https://img.shields.io/badge/Min%20SDK-26%20%28Android%208.0%2B%29-brightgreen.svg)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35%20%28Android%2015%29-blue.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin%20100%25-purple.svg)](https://kotlinlang.org/)
[![UI Framework](https://img.shields.io/badge/UI-Jetpack%20Compose-darkgreen.svg)](https://developer.android.com/jetpack/compose)

*SentinelShield is a privacy-first, enterprise-grade Android anti-theft security solution designed to counter modern phone theft tactics, prevent physical shutdown attempts, capture intruder evidence, and provide remote command capabilities.*

</div>

---

## Key Security Features

### Fake Power Menu & Decoy Shutdown Interception
* **Pre-Emptive Power Interception:** Intercepts physical long-press power button events before the native Android system power menu pops up.
* **Realistic Stock Power Dialog:** Displays a stock Pixel-style power options menu (**Power off**, **Restart**, **Lockdown**, **Emergency**).
* **Decoy Pitch-Black Screen:** When a thief taps "Power off" or "Restart", SentinelShield executes a heavy haptic vibration pulse and transitions to a pitch-black fullscreen overlay (`DecoyScreenActivity`), tricking the thief into believing the device powered down while all protection services remain 100% active in the background.

### Pocket Snatch Protection
* **Sensor-Based Detection:** Utilizes device proximity and accelerometer motion sensors to detect unauthorized removal from pockets or bags.
* **Configurable Arming & Grace Period:** Allows customizable arming delays (5s) and disarm grace periods (3s) to prevent false alerts.
* **Emergency Alarm:** Triggers an un-silenceable max-volume siren audio loop (`SecurityAlertService`) with optional strobe flash alerts.

### Charging Disconnect Monitor
* **Hardware Power Monitoring:** Monitors USB/wireless hardware power connection status in real-time.
* **Immediate Unplug Alert:** Triggers an immediate alarm if the charging cable is disconnected while armed.

### SIM Tamper & State Monitor
* **SIM Change Detection:** Listens to hardware SIM state broadcasts via `TelephonyManager` and `SubscriptionManager`.
* **Instant Emergency Notification:** Instantly raises security alerts and sends SMS notifications if a SIM card is removed or hot-swapped.

### Intruder Selfie & Evidence Capture
* **CameraX Stealth Recording:** Captures stealth front-camera photos or 3-second HD videos when invalid lockscreen password attempts occur.
* **Local & Cloud Storage:** Automatically stores timestamped evidence locally and queues background uploads.

### Google Drive Cloud Backup
* **Cloud Integration:** Seamless integration with Google Drive API.
* **Automatic Evidence Sync:** Automatically syncs intruder photos, videos, and security diagnostic logs to a dedicated `SentinelShield` cloud folder.

### Remote SMS Control Suite
* **SMS Command Interception:** Intercepts incoming SMS commands sent from user-authorized trusted contact numbers.
* **Multi-Alias Command Set:**
  * **Screen Lock:** `LOCK`, `LOCKDOWN`, or `LOST` — Instantly locks the device screen via Device Admin Policy.
  * **Siren Alarm:** `SIREN`, `ALARM`, `SOUND`, or `RING` — Triggers the max-volume emergency siren remotely with continuous volume override.
  * **GPS Location:** `LOCATION`, `TRACK`, `GPS`, `LOCATE`, or `WHERE` — Automatically enables Location Services and Mobile Data in one go (via `WRITE_SECURE_SETTINGS`), executes a 2-second settling window for GPS satellite locking, and sends back an official Google Maps Location Sharing link (`https://maps.google.com/maps?q=loc:LAT,LNG&z=17`).
* **Auto Location & Data Toggle:** Includes ADB system permission helper for `WRITE_SECURE_SETTINGS` (`adb shell pm grant com.sentinelshield.antitheft android.permission.WRITE_SECURE_SETTINGS`) with copiable command dialogs.
* **Remote SMS Diagnostics Panel:** In-app diagnostic card to verify SMS permissions, secure settings status, and log history.

### WearOS Smartwatch Companion
* **Wrist Companion Module:** Features a dedicated WearOS companion module (`:wear`) for remote alarm triggering, status monitoring, and haptic alerts right from your wrist.

---

## Design System & User Experience

Built from the ground up using **Material 3 & Jetpack Compose**:
* **Dynamic Color Tokens:** Supports Android Material You dynamic color extraction and custom HSL color palettes.
* **Pure Dark & AMOLED Modes:** Optimized for OLED displays to save battery and offer high-contrast night viewing.
* **Haptic & Audio Engineering:** Features continuous volume enforcement (polling every 500ms), `PARTIAL_WAKE_LOCK` CPU hold, transient exclusive audio focus, and self-healing `MediaPlayer` error recovery.

---

## Architecture & Technology Stack

```
   ┌─────────────────────────────────────────────────────────┐
   │                  SentinelShield Core                    │
   └────────────────────────────┬────────────────────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                       ▼
┌───────────────┐       ┌───────────────┐       ┌───────────────┐
│ Jetpack       │       │ Foreground    │       │ Hardware      │
│ Compose UI    │       │ Services      │       │ Interceptors  │
│ (M3 Theme)    │       │ (Sticky FGS)  │       │ (Accessibility│
└───────────────┘       └───────────────┘       │  & Sensors)   │
                                                └───────────────┘
```

* **Architecture Pattern:** MVVM (Model-View-ViewModel) + Clean Architecture.
* **UI Engine:** 100% Jetpack Compose with Material 3 components.
* **Background Processing:** Android Foreground Services (`START_STICKY`, `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`) with CPU `WakeLock` protection.
* **Hardware Interception:** Custom `AccessibilityService` (`PowerButtonAccessibilityService`) for global key event listening.
* **Asynchronous Execution:** Kotlin Coroutines & `StateFlow`.
* **Hardware APIs:** CameraX, SensorManager, TelephonyManager, AudioManager, FusedLocationProviderClient.

---

## Building & Running from Source

### Prerequisites
* **Android Studio:** Ladybug (2024.2.1+) or newer.
* **JDK:** Version 17+.
* **Android SDK:** API Level 35 (Android 15) installed.

### Step-by-Step Setup

1. **Clone the Repository:**
   ```bash
   git clone git@github.com:ShivaSchauhan/SentinelShield.git
   cd SentinelShield
   ```

2. **Open in Android Studio:**
   * Launch Android Studio, select **Open**, and navigate to the project directory.

3. **Build Debug APK via Command Line:**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on Connected Device / Emulator:**
   ```bash
   ./gradlew installDebug
   ```

---

## Privacy & Security Commitment

* **Zero Telemetry:** SentinelShield contains zero third-party tracking, analytics, or data harvesting SDKs.
* **Local-First Data Storage:** All security logs, photos, and configurations remain strictly on your device or your personal Google Drive account.
* **Explicit Disarming:** Alarms can only be disarmed when the legitimate owner unlocks the device via native PIN, Pattern, or Fingerprint authentication.
