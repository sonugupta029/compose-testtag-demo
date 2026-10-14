package io.github.testidspike

import androidx.compose.ui.Modifier

/**
 * Modifier extension that enables testTag-based IDs for Appium.
 *
 * - Android: applies semantics { testTagsAsResourceId = true }
 * - iOS: no-op (CMP 1.11+ maps testTag to accessibilityIdentifier by default)
 */
expect fun Modifier.platformTestTagModifier(): Modifier
