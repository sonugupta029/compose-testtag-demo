# Investigation: Can Same testTag ID Work on Both iOS and Android via Appium?

**Document Version:** 2.0 | **Date:** October 14, 2026 | **Author:** Sonu Gupta
**Status:** ✅ Validated | **Project:** `compose-testtag-demo`  
**CMP Version:** 1.11.0 | **Kotlin:** 2.4.0

---

## Objective

Investigate whether a single `Modifier.testTag("id")` in Compose Multiplatform shared code produces the **same identifier** on both Android and iOS that Appium can locate using one locator strategy.

**Question:** Can `AppiumBy.accessibilityId("Auth-Login-Button-signIn")` find the same element on both platforms?

**Answer:** ✅ **Yes.**

---

## How It Works

In CMP, `Modifier.testTag("...")` is set **once in shared code**:

```kotlin
// commonMain — written once
Button(
    modifier = Modifier.testTag("Auth-Login-Button-signIn"),
    onClick = { /* ... */ }
) { Text("Sign In") }
```

This maps to:

| Platform | Native Attribute | Appium Locator |
|----------|-----------------|----------------|
| Android | `resource-id` | `AppiumBy.accessibilityId("Auth-Login-Button-signIn")` |
| iOS | `accessibilityIdentifier` | `AppiumBy.accessibilityId("Auth-Login-Button-signIn")` |

**Same locator, same ID, both platforms.**

---

## Required Configuration

### Android: `testTagsAsResourceId = true`

Without this, `testTag` is internal to Compose and invisible to Appium/UiAutomator2.

```kotlin
// androidMain/PlatformTestTag.android.kt
actual fun Modifier.platformTestTagModifier(): Modifier {
    return this.semantics { testTagsAsResourceId = true }
}
```

Applied once at the root `App.kt` via a wrapping `Surface` — all screens and child elements inherit it automatically:

```kotlin
// commonMain/App.kt
@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize().platformTestTagModifier()) {
            TestIdSpikeScreen()  // All testTags inside are exposed to Appium
        }
    }
}
```

> **Note:** Previously we recommended applying this on each screen's `Scaffold`. The root-level approach is better because it's set once and covers the entire app — including elements outside Scaffold like navigation drawers, dialogs, and bottom sheets.

### iOS: Nothing (CMP 1.11+)

`testTag` maps to `accessibilityIdentifier` automatically. No configuration needed.

```kotlin
// iosMain/MainViewController.kt — that's it
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
```

---

## Build Configuration

| Configuration | All Builds (QA, Prod) |
|---------------|----------------------|
| `testTagsAsResourceId` (Android) | `true` — no performance impact |
| iOS accessibility | Automatic (CMP 1.11+ syncs tree by default) |

Automation runs on both QA and Production — same config for both environments.

---

## testTag Naming Convention

**Format:** `FeatureName-ScreenName-ElementType-uniqueName`

| Segment | Case | Example |
|---------|------|---------|
| FeatureName | PascalCase | `Auth`, `Discovery`, `RouterList`, `Settings` |
| ScreenName | PascalCase | `Login`, `Landing`, `Details`, `Env` |
| ElementType | PascalCase | `Button`, `TextField`, `Switch`, `Checkbox`, `Card`, `Text`, `Dropdown` |
| uniqueName | camelCase | `signIn`, `email`, `rememberMe`, `biometric` |

### Rules
- IDs are **static** — never contain dynamic/runtime values
- Exception: list items with known model IDs (e.g., `RouterList-Item-Card-E3000`)
- Every interactive Composable MUST have a `testTag` (enforced via PR review)
- Non-interactive elements (labels, headers) should also have `testTag` if they need verification

### Examples

| Element | testTag |
|---------|---------|
| Email input | `Auth-Login-TextField-email` |
| Sign In button | `Auth-Login-Button-signIn` |
| SSO button | `Auth-Login-Button-ssoLogin` |
| Remember Me checkbox | `Auth-Login-Checkbox-rememberMe` |
| Biometric switch | `Auth-Login-Switch-biometric` |
| Router card in list | `RouterList-Item-Card-E3000` |
| Scan device button | `Discovery-Landing-Button-scanDevice` |

---

## Validated Element Types

Tested with Appium Inspector on **physical Android (Pixel 9a) and iOS (iPhone 15)** devices:

| Element Type | Compose Component | Android ✅ | iOS ✅ |
|---|---|---|---|
| Text input | `OutlinedTextField` | ✅ | ✅ |
| Filled button | `Button` | ✅ | ✅ |
| Outlined button | `OutlinedButton` | ✅ | ✅ |
| Text button | `TextButton` | ✅ | ✅ |
| Checkbox | `Checkbox` | ✅ | ✅ |
| Switch/Toggle | `Switch` | ✅ | ✅ |
| Static text | `Text` | ✅ | ✅ |
| Card container | `Card` | ✅ | ✅ |
| List items | `LazyColumn` items | ✅ | ✅ |
| Dropdown | `ExposedDropdownMenuBox` | ✅ | ✅ |

---

## What This Means for Automation

### Current (Native Apps)

| Aspect | How it works today |
|--------|-------------------|
| Repos | 2 separate (`ncm_mobile_test_automation`, `verify_mobile_test_automation`) |
| IDs per element | 2 annotations (`@AndroidFindBy` + `@iOSXCUITFindBy`) |
| Locator strategies | Mixed (resource ID, accessibility, classChain, XPath, label) |
| ID ownership | Split between iOS dev and Android dev — often different |
| Fragile locators | Common on iOS (label-based, classChain) |

### Compose Multiplatform CMP App

| Aspect | How it will work |
|--------|-----------------|
| Repos | **1** unified automation repo |
| IDs per element | **1** locator — `accessibilityId` |
| Locator strategies | **Single** — `AppiumBy.accessibilityId("...")` for everything |
| ID ownership | Dev team sets once in shared code — same on both platforms |
| Fragile locators | **Eliminated** — no XPath, no classChain, no label matching |

### New POM Pattern (using existing framework)

Keeping the current `@AndroidFindBy` + `@iOSXCUITFindBy` page factory pattern — but now both annotations have the **same value**:

```kotlin
class LoginScreen(val mobileDriver: AppiumDriver?) : BaseTestPage(mobileDriver) {

    // BEFORE (native apps — different IDs):
    // @AndroidFindBy(id = "${Util.appPackage}log_bt_login")
    // @iOSXCUITFindBy(id = "log_bt_login")

    // AFTER (CMP — same ID on both):
    @AndroidFindBy(accessibility = "Auth-Login-Button-signIn")
    @iOSXCUITFindBy(accessibility = "Auth-Login-Button-signIn")
    private val loginButton: WebElement? = null

    @AndroidFindBy(accessibility = "Auth-Login-TextField-email")
    @iOSXCUITFindBy(accessibility = "Auth-Login-TextField-email")
    private val emailTextField: WebElement? = null

    @AndroidFindBy(accessibility = "Auth-Login-TextField-password")
    @iOSXCUITFindBy(accessibility = "Auth-Login-TextField-password")
    private val passwordTextField: WebElement? = null

    @AndroidFindBy(accessibility = "Auth-Login-Button-ssoLogin")
    @iOSXCUITFindBy(accessibility = "Auth-Login-Button-ssoLogin")
    private val signInWithSSOButton: WebElement? = null

    @AndroidFindBy(accessibility = "Auth-Login-Checkbox-rememberMe")
    @iOSXCUITFindBy(accessibility = "Auth-Login-Checkbox-rememberMe")
    private val rememberMeCheckbox: WebElement? = null

    init {
        initPageElements()
    }

    fun login(email: String, password: String) {
        sendKeys(emailTextField, email)
        sendKeys(passwordTextField, password)
        tap(loginButton)
    }
}
```

**Key changes from current pattern:**
- `id = "${Util.appPackage}..."` → `accessibility = "..."` (no app package prefix needed)
- Both annotations have **identical values** (no more ID mismatch between platforms)
- No more fragile `iOSClassChain` or `XPath` locators — everything is `accessibility`

---

## Dev Team Requirements (Definition of Done)

| # | Requirement | Owner | When |
|---|---|---|---|
| 1 | Every interactive Composable has `Modifier.testTag("Convention-Id")` | Dev | Every PR |
| 2 | Root `App.kt` applies `platformTestTagModifier()` on wrapping `Surface` (covers entire app) | Dev | App setup (once) |
| 3 | testTag naming follows `FeatureName-ScreenName-ElementType-uniqueName` | Dev | Every PR |
| 4 | No dynamic values in testTags (except list item model IDs) | Dev | Every PR |
| 5 | PR reviewer verifies testTags are present on new interactive elements | Dev/QA | Every PR |

---

## Open Questions / Future Work

| # | Question | Status | Answer |
|---|----------|--------|--------|
| 1 | Should we add a lint rule to fail build if `testTag` is missing on interactive elements? | To discuss | — |
| 2 | How to handle modal dialogs / bottom sheets? | To validate | — |
| 3 | WebView elements (SSO login) | Known limitation | Use web locators — testTag doesn't apply inside WebView |

---

## Test Devices Used

| Device | Platform |
|--------|----------|
| Pixel 9a | Android |
| iPhone 15 | iOS 17.5 |

---

## Project Layout

```
compose-testtag-demo/
├── composeApp/src/
│   ├── commonMain/    → TestIdSpikeScreen.kt (ALL UI code)
│   ├── androidMain/   → MainActivity.kt, PlatformTestTag.android.kt
│   └── iosMain/       → MainViewController.kt, PlatformTestTag.ios.kt
├── iosApp/            → Xcode wrapper (Apple requirement)
├── ComposeTestTagDemo.ipa          → iOS build
└── composeApp/build/outputs/apk/debug/composeApp-debug.apk → Android build
```

---

## References

- [JetBrains: Compose iOS Accessibility](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-ios-accessibility.html)
- [CMP 1.11.0 What's New](https://kotlinlang.org/docs/multiplatform/whats-new-compose-111.html)

---

*Investigation completed: October 14, 2026 | Compose Multiplatform 1.11.0 | Kotlin 2.4.0*
