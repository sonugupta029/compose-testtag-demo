# How One Codebase Makes Two Apps — Explained Simply

## The Analogy

Think of it like a **recipe book** (commonMain) and two **kitchens** (Android + iOS):

- The recipe (UI code) is the same
- Kitchen A (Android) uses an oven → produces a cake (APK)
- Kitchen B (iOS) uses a different oven → produces the same cake (IPA)
- Both cakes taste identical because the recipe was the same

---

## Our Project Files — What Each One Does

```
compose-testtag-demo/
│
├── 📁 composeApp/src/
│   │
│   ├── 📁 commonMain/ ←────────── THE RECIPE (95% of code lives here)
│   │   ├── App.kt                 "Start the app, apply platform config, show screens"
│   │   ├── TestIdSpikeScreen.kt   "Here's the UI — buttons, fields, lists"
│   │   └── PlatformTestTag.kt     "I need a platform thing (expect)"
│   │
│   ├── 📁 androidMain/ ←───────── ANDROID-SPECIFIC (just entry + config)
│   │   ├── MainActivity.kt        "Android, please launch App()"
│   │   └── PlatformTestTag.android.kt  "Here's how Android does that platform thing"
│   │
│   └── 📁 iosMain/ ←───────────── iOS-SPECIFIC (just entry + config)
│       ├── MainViewController.kt  "iOS, please launch App()"
│       └── PlatformTestTag.ios.kt "Here's how iOS does that platform thing (nothing needed)"
│
├── 📁 iosApp/ ←─────────────────── XCODE PACKAGING (Apple requires this wrapper)
│   ├── iOSApp.swift               "SwiftUI says: show the Compose screen"
│   ├── ContentView.swift          "Bridge between Swift world → Kotlin world"
│   └── Info.plist                 "App name, version, permissions"
│
├── 📄 build.gradle.kts ←────────── BUILD INSTRUCTIONS
│   (tells Gradle: "compile commonMain for Android AND iOS")
│
├── 📦 composeApp-debug.apk ←────── OUTPUT: Android app
└── 📦 ComposeTestTagDemo.ipa ←───────────────── OUTPUT: iOS app
```

---

## The Build Flow (Step by Step)

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│   YOU WRITE CODE HERE (once):                                   │
│                                                                 │
│   commonMain/TestIdSpikeScreen.kt                               │
│   ┌───────────────────────────────────────────┐                 │
│   │  Button(                                  │                 │
│   │    modifier = Modifier.testTag(           │                 │
│   │      "Auth-Login-Button-signIn"           │                 │
│   │    ),                                     │                 │
│   │    onClick = { login() }                  │                 │
│   │  ) { Text("Sign In") }                   │                 │
│   └───────────────────────────────────────────┘                 │
│                           │                                     │
└───────────────────────────┼─────────────────────────────────────┘
                            │
              ┌─────────────┴─────────────┐
              │                           │
              ▼                           ▼
┌──────────────────────┐    ┌──────────────────────┐
│   ANDROID PATH       │    │   iOS PATH           │
│                      │    │                      │
│ 1. Kotlin Compiler   │    │ 1. Kotlin/Native     │
│    (JVM bytecode)    │    │    Compiler          │
│         │            │    │    (native binary)   │
│         ▼            │    │         │            │
│ 2. Android Gradle    │    │         ▼            │
│    (bundles into APK)│    │ 2. Creates           │
│         │            │    │    ComposeApp        │
│         ▼            │    │    .framework        │
│ 3. Signs with        │    │         │            │
│    keystore          │    │         ▼            │
│         │            │    │ 3. Xcode links       │
│         ▼            │    │    framework into    │
│                      │    │    iosApp/ shell     │
│  ┌──────────────┐    │    │         │            │
│  │              │    │    │         ▼            │
│  │  📱 APK     │    │    │ 4. Signs with        │
│  │              │    │    │    Apple cert        │
│  │  Install via │    │    │         │            │
│  │  adb install │    │    │         ▼            │
│  │              │    │    │                      │
│  └──────────────┘    │    │  ┌──────────────┐    │
│                      │    │  │              │    │
│  Activity:           │    │  │  📱 IPA     │    │
│  MainActivity.kt     │    │  │              │    │
│  "Launch App()"      │    │  │  Install via │    │
│                      │    │  │  devicectl   │    │
└──────────────────────┘    │  │              │    │
                            │  └──────────────┘    │
                            │                      │
                            │  Entry:              │
                            │  iOSApp.swift →      │
                            │  ContentView.swift → │
                            │  MainViewController  │
                            │  "Launch App()"      │
                            └──────────────────────┘
              │                           │
              ▼                           ▼
┌──────────────────────────────────────────────────┐
│                                                  │
│  BOTH APPS SHOW THE EXACT SAME SCREEN:           │
│                                                  │
│  ┌────────────────────────────────────────┐      │
│  │  TestTag Demo                           │      │
│  │  ┌──────────────────────────────┐      │      │
│  │  │ Email                        │      │      │
│  │  └──────────────────────────────┘      │      │
│  │  ┌──────────────────────────────┐      │      │
│  │  │ Password                     │      │      │
│  │  └──────────────────────────────┘      │      │
│  │  ☐ Remember Me                         │      │
│  │  ┌──────────────────────────────┐      │      │
│  │  │        Sign In               │      │      │
│  │  └──────────────────────────────┘      │      │
│  └────────────────────────────────────────┘      │
│                                                  │
│  testTag "Auth-Login-Button-signIn" visible on   │
│  BOTH platforms via Appium Inspector ✅           │
│                                                  │
└──────────────────────────────────────────────────┘
```

---

## Commands That Produce Each App

```bash
# ANDROID — one command
./gradlew :composeApp:assembleDebug
# Output: composeApp/build/outputs/apk/debug/composeApp-debug.apk

# iOS — two steps (Gradle builds framework, Xcode packages it)
cd iosApp && xcodebuild -project iosApp.xcodeproj -scheme ComposeTestTagDemo ...
# Output: iosApp/build/Build/Products/Debug-iphoneos/ComposeTestTagDemo.app
```

---

## Why iosApp/ Exists But No "androidApp/" Folder

| Android | iOS |
|---------|-----|
| Gradle can produce APK directly | Apple REQUIRES Xcode to produce .app/.ipa |
| `MainActivity.kt` is inside `composeApp/src/androidMain/` | Need a separate Xcode project (`iosApp/`) as a shell |
| No extra wrapper needed | 3 Swift files bridge into Compose |

**In a Compose Multiplatform app, devs will almost never touch `iosApp/`** — it's set up once and forgotten. All real work happens in `commonMain/`.

---

## The "expect/actual" Pattern (Platform-Specific Bits)

When we need something different per platform (rare — maybe 5% of code):

```
commonMain/PlatformTestTag.kt          → "I EXPECT a function called platformTestTagModifier()"
androidMain/PlatformTestTag.android.kt → "Here's the ACTUAL: enable testTagsAsResourceId"
iosMain/PlatformTestTag.ios.kt         → "Here's the ACTUAL: do nothing (iOS handles it)"
```

Same pattern used for: Camera, GPS, Maps, Barcode scanner, Biometric, etc.
The function signature is identical — only the internals differ per platform.

---

## TL;DR

| What | Where | How many times written |
|------|-------|----------------------|
| UI screens, buttons, logic | `commonMain/` | **Once** |
| Android app entry | `androidMain/MainActivity.kt` | Once (boilerplate) |
| iOS app entry | `iosMain/MainViewController.kt` + `iosApp/*.swift` | Once (boilerplate) |
| Platform features (camera etc.) | `expect/actual` | Once per platform |
| **Total unique UI code** | | **Written once, runs everywhere** |
