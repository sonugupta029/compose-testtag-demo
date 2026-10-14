# Compose TestTag / AccessibilityId Demo

Minimal Compose Multiplatform project that proves `Modifier.testTag()` produces 
the **same accessibilityId on both Android and iOS** for Appium automation.

## Why This Project Exists

When an app ships separate native codebases for Android and iOS, UI test automation
usually has to maintain **two different locators for every element** — a `resource-id`
on Android and a different `accessibilityIdentifier` (often a label or XPath) on iOS.
That means two sets of IDs, two places to update, and locators that drift apart over time.

Moving to **Compose Multiplatform** means the UI is written once in shared code. The
open question was: can we also define each element's automation ID **once** in that
shared code and have it surface **identically to Appium on both platforms** — so QA
can use a single locator strategy instead of two?

This project is the spike that answers that question. It builds a small Compose
Multiplatform screen with every common element type, tags each one with a single
`Modifier.testTag("...")`, and verifies that **the same
`AppiumBy.accessibilityId("...")` locates the element on both Android and iOS.**

## Result: PASSED

**Same `AppiumBy.accessibilityId("Auth-Login-Button-signIn")` works on both platforms.**

---

## Key Findings

### 1. Android — Requires `testTagsAsResourceId = true`

Without this, `testTag` is only visible in Compose UI tests, NOT to Appium/UiAutomator2.

Applied **once at the root `App.kt`** level — all screens inherit it automatically:

```kotlin
// commonMain/App.kt — applied once, covers entire app
@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize().platformTestTagModifier()) {
            // All screens go here — testTags are exposed automatically
        }
    }
}
```

The `platformTestTagModifier()` uses expect/actual:
```kotlin
// androidMain — enables resource ID exposure
actual fun Modifier.platformTestTagModifier(): Modifier {
    return this.semantics { testTagsAsResourceId = true }
}

// iosMain — no-op (CMP 1.11+ handles it automatically)
actual fun Modifier.platformTestTagModifier(): Modifier = this
```

### 2. iOS — Works Out of the Box (CMP 1.11+)

No configuration needed. CMP 1.11+ syncs the accessibility tree automatically. `testTag` maps to `accessibilityIdentifier` by default.

```kotlin
// iosMain — MainViewController.kt (that's it — no config!)
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
```

> **Note:** On CMP 1.7.x, you needed `AccessibilitySyncOptions.Always` + experimental opt-in. Not needed on 1.11+.

### 3. Single Locator Strategy Works on Both Platforms

| Platform | How testTag is exposed | Appium locator |
|----------|----------------------|----------------|
| Android | `resource-id` (via `testTagsAsResourceId`) | `AppiumBy.accessibilityId("...")` |
| iOS | `accessibilityIdentifier` (automatic in CMP 1.11+) | `AppiumBy.accessibilityId("...")` |

---

## Impact on Automation Architecture

### Before (Native — 2 repos, 2 IDs per element)
```kotlin
@AndroidFindBy(id = "${appPackage}log_bt_login")
@iOSXCUITFindBy(id = "log_bt_login")          // often DIFFERENT from Android
private val loginButton: WebElement? = null
```

### After (CMP — 1 repo, 1 ID per element)
```kotlin
// Single annotation works for both platforms!
@FindBy(accessibility = "Auth-Login-Button-signIn")
private val loginButton: WebElement? = null
```

Or directly:
```kotlin
driver.findElement(AppiumBy.accessibilityId("Auth-Login-Button-signIn"))
```

---

## Convention

`FeatureName-ScreenName-ElementType-uniqueName`

- PascalCase for Feature, Screen, ElementType
- camelCase for uniqueName  
- IDs are **static** — never contain dynamic/runtime values
- Exception: list items with known model IDs (e.g., `RouterList-Item-Card-E3000`)

### Examples
```
Auth-Login-TextField-email
Auth-Login-Button-signIn
Auth-Login-Checkbox-rememberMe
Auth-Login-Switch-biometric
Discovery-Landing-Button-scanDevice
RouterList-Item-Card-E3000
Settings-Env-Dropdown-selector
```

---

## Elements Validated

| Element Type | testTag | Android ✅ | iOS ✅ |
|---|---|---|---|
| TextField | `Auth-Login-TextField-email` | ✅ | ✅ |
| Button | `Auth-Login-Button-signIn` | ✅ | ✅ |
| OutlinedButton | `Auth-Login-Button-ssoLogin` | ✅ | ✅ |
| TextButton | `Auth-Login-Button-forgotPassword` | ✅ | ✅ |
| Checkbox | `Auth-Login-Checkbox-rememberMe` | ✅ | ✅ |
| Switch | `Auth-Login-Switch-biometric` | ✅ | ✅ |
| Text | `Auth-Login-Text-screenTitle` | ✅ | ✅ |
| Card | `Auth-Login-Card-loginStatus` | ✅ | ✅ |
| LazyColumn items | `RouterList-Item-Card-E3000` | ✅ | ✅ |
| Dropdown | `Settings-Env-Dropdown-selector` | ✅ | ✅ |

---

## Dev DoD Requirements (for a Compose Multiplatform app)

Based on this spike, the following must be enforced in a Compose Multiplatform app:

1. **Every interactive Composable** must have `Modifier.testTag("Convention-Based-Id")`
2. **Root `App.kt`** must apply `platformTestTagModifier()` on a wrapping `Surface` — this enables `testTagsAsResourceId` for the entire app (no per-screen setup needed)
3. **iOS entry point** must use `AccessibilitySyncOptions.Always` (for QA/test builds) or `.WhenRequiredByAccessibilityServices` (for production)
4. **Automation repo** can be a single repo with single POM per screen — no dual annotations needed

---

## Build Configuration

| Config | All Builds (QA, Prod) |
|--------|----------------------|
| `AccessibilitySyncOptions` | `.Always` — automation runs on both QA and Prod |
| `testTagsAsResourceId` | `true` (no perf impact) |

---

## Build & Run

### Android
```bash
./gradlew :composeApp:assembleDebug
adb install composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

### iOS (Real Device)
```bash
cd iosApp && xcodebuild -project iosApp.xcodeproj -scheme ComposeTestTagDemo -configuration Debug \
  -destination 'generic/platform=iOS' -derivedDataPath build -allowProvisioningUpdates clean build
xcrun devicectl device install app --device <UDID> iosApp/build/Build/Products/Debug-iphoneos/ComposeTestTagDemo.app
```

### Appium Capabilities

**Android:**
```json
{
  "platformName": "Android",
  "appium:automationName": "UiAutomator2",
  "appium:appPackage": "io.github.testidspike",
  "appium:appActivity": "io.github.testidspike.MainActivity"
}
```

**iOS:**
```json
{
  "platformName": "iOS",
  "appium:automationName": "XCUITest",
  "appium:udid": "<device udid>",
  "appium:bundleId": "io.github.testidspike"
}
```

---

## References

- [JetBrains: Compose iOS Accessibility](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-ios-accessibility.html)
- [Compose Multiplatform 1.6.0 Release (Accessibility)](https://blog.jetbrains.com/kotlin/2024/02/compose-multiplatform-1-6-0-release)

---

## License

Licensed under the [Apache License 2.0](LICENSE). Copyright 2026 Sonu Gupta.

---

*Spike validated: Oct 14, 2026 | By: Sonu Gupta*
