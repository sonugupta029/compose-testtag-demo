import Foundation

// This file ensures the ComposeApp.framework is properly linked.
// The framework is built by Gradle KMP and output to:
// composeApp/build/xcode-frameworks/{CONFIGURATION}/{SDK_NAME}/ComposeApp.framework
//
// To build and run:
// 1. Run: ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
//    (This is typically set as a Build Phase "Run Script" in Xcode)
// 2. Open iosApp.xcodeproj in Xcode
// 3. Build and Run on simulator or device
