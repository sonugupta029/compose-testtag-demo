package io.github.testidspike

import androidx.compose.ui.Modifier

/**
 * On iOS, testTag maps to accessibilityIdentifier by default (CMP 1.11+).
 * No additional configuration needed.
 */
actual fun Modifier.platformTestTagModifier(): Modifier = this
