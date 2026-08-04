# Sentinel Shield architecture

## Goal

Build a transparent, local-first anti-theft helper for a phone its owner administers. The app should raise useful alerts without pretending to have system privileges, collecting identifiers, or maintaining a high-drain sensor loop.

## Supported consumer-app features

| Area | Design | Constraint |
| --- | --- | --- |
| SIM tamper alert | A user arms a visible, `specialUse` foreground monitor. It listens for subscription changes and uses the legacy SIM-state broadcast only as a best-effort fallback. | Android/carrier/OEM behaviour varies; this is neither an ICCID match nor eSIM protection. |
| Audible alert | A short-lived `mediaPlayback` foreground service uses public alarm audio attributes and asks for Notification Policy access before restoring normal ringer mode. | The OS and user DND policy remain authoritative. |
| Lock-screen privacy | An opt-in active device-admin policy requests the public `disable-keyguard-features` policy before hiding secure notification content. | This does not restrict Quick Settings, and OEM behaviour varies. |
| Screen shield | A manually opened black activity is exited with the device credential. | It is not a shutdown, cannot intercept hardware keys, and is intentionally off by default because the display consumes battery. |


## Battery budget strategy

The design makes no unverified “under 5% per day” promise. It should instead be measured on physical devices with Android Studio’s Energy Profiler and `adb shell dumpsys batterystats` after each release. The code avoids polling, location collection, wake locks, repeating work, and network traffic. The only persistent work while armed is a user-visible event listener; the alarm service exists only during an alert.

## Enterprise boundary

Blocking Quick Settings, kiosk-style lock task, remote wipe, and forced lock policies belong to an Android Enterprise Device Owner deployment. Device Owner provisioning is a separate product track and must be done during supported provisioning flows; it is not a runtime escalation for a personal app.

## Sources

- [Android Gradle Plugin Kotlin compatibility](https://developer.android.com/build/kotlin-support)
- [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types)
- [DevicePolicyManager keyguard policies](https://developer.android.com/reference/android/app/admin/DevicePolicyManager)
- [Google Play Accessibility API policy](https://support.google.com/googleplay/android-developer/answer/17190352)
