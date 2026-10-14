plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
}

// ---------------------------------------------------------------------------
// "cmp run" tasks — one-click run of the app on the right platform.
// These show up in the Gradle tool window under the "cmp run" group and are
// wired to the IDE run configurations "androidApp" / "iOSApp".
//   - runAndroid : install + launch on an Android device OR emulator
//   - runIos     : build + launch on a connected iOS device, else a Simulator
// ---------------------------------------------------------------------------

val cmpRunGroup = "cmp run"

// Resolve the Android SDK location (local.properties -> ANDROID_HOME -> default).
val androidSdkDir: String = run {
    val props = java.util.Properties()
    val localProps = rootProject.file("local.properties")
    if (localProps.exists()) {
        localProps.inputStream().use { props.load(it) }
    }
    props.getProperty("sdk.dir")
        ?: System.getenv("ANDROID_HOME")
        ?: System.getenv("ANDROID_SDK_ROOT")
        ?: "${System.getProperty("user.home")}/Library/Android/sdk"
}

tasks.register<Exec>("runAndroid") {
    group = cmpRunGroup
    description = "Install and launch the app on a connected Android device or emulator."
    // Build + install the debug APK first.
    dependsOn(":composeApp:installDebug")
    val adb = file("$androidSdkDir/platform-tools/adb").absolutePath
    // applicationId/.activity from composeApp AndroidManifest.
    commandLine(
        adb, "shell", "am", "start",
        "-n", "io.github.testidspike/io.github.testidspike.MainActivity"
    )
}

tasks.register<Exec>("runIos") {
    group = cmpRunGroup
    description = "Build and launch the app on a connected iOS device, or an iOS Simulator if none."
    workingDir = rootDir
    commandLine("bash", "scripts/run-ios.sh")
}
