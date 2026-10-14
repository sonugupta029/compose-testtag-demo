package io.github.testidspike

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

actual fun Modifier.platformTestTagModifier(): Modifier {
    return this.semantics { testTagsAsResourceId = true }
}
