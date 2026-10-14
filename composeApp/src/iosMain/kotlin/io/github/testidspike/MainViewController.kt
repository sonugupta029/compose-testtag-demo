package io.github.testidspike

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

// CMP 1.8+ syncs accessibility tree lazily by default
// No AccessibilitySyncOptions needed!
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
