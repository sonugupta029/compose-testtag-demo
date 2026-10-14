#!/usr/bin/env bash
# Build and run the CMP iOS app.
# Auto-detects target: uses a connected physical iPhone if one is attached &
# trusted, otherwise falls back to an iOS Simulator.
# Invoked by the Gradle task :runIos, but can also be run directly.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

PROJECT="iosApp/iosApp.xcodeproj"
SCHEME="ComposeTestTagDemo"
CONFIG="Debug"
BUNDLE_ID="io.github.testidspike"

# Keep the Xcode project in sync with project.yml (source of truth).
if command -v xcodegen >/dev/null 2>&1; then
  echo ">> Regenerating Xcode project from project.yml"
  (cd iosApp && xcodegen generate)
fi

# --- Detect a connected physical iOS device ------------------------------
DEVICE_ID="$(xcrun xctrace list devices 2>/dev/null \
  | awk '/^== Devices ==/{f=1;next} /^== /{f=0} f' \
  | grep -vi 'simulator' \
  | grep -Eo '\([0-9A-Fa-f-]{25,}\)' \
  | tr -d '()' \
  | head -1 || true)"

if [ -n "${DEVICE_ID}" ]; then
  # ---------------------- REAL DEVICE ------------------------------------
  echo ">> Physical device detected: ${DEVICE_ID}"
  DERIVED="build/ios-device"
  echo ">> Building ${SCHEME} for the device"
  xcodebuild \
    -project "${PROJECT}" \
    -scheme "${SCHEME}" \
    -configuration "${CONFIG}" \
    -sdk iphoneos \
    -destination "id=${DEVICE_ID}" \
    -derivedDataPath "${DERIVED}" \
    -allowProvisioningUpdates \
    build

  APP_PATH="$(find "${DERIVED}/Build/Products" -maxdepth 2 -name '*.app' | head -1)"
  [ -n "${APP_PATH}" ] || { echo "!! No .app built under ${DERIVED}/Build/Products" >&2; exit 1; }

  echo ">> Installing ${APP_PATH} on device"
  xcrun devicectl device install app --device "${DEVICE_ID}" "${APP_PATH}"
  echo ">> Launching ${BUNDLE_ID}"
  xcrun devicectl device process launch --device "${DEVICE_ID}" "${BUNDLE_ID}"
  echo ">> Done (device)."
  exit 0
fi

# ---------------------- SIMULATOR --------------------------------------
echo ">> No physical device connected; using an iOS Simulator."
DERIVED="build/ios-sim"

BOOTED="$(xcrun simctl list devices booted | grep -Eo '[0-9A-Fa-f-]{36}' | head -1 || true)"
if [ -z "${BOOTED}" ]; then
  echo ">> No booted simulator; booting the first available iPhone..."
  BOOTED="$(xcrun simctl list devices available | grep -E 'iPhone' | grep -Eo '[0-9A-Fa-f-]{36}' | head -1)"
  [ -n "${BOOTED}" ] || { echo "!! No available iPhone simulator. Create one in Xcode > Settings > Platforms." >&2; exit 1; }
  xcrun simctl boot "${BOOTED}"
fi
open -a Simulator

echo ">> Building ${SCHEME} for the simulator"
xcodebuild \
  -project "${PROJECT}" \
  -scheme "${SCHEME}" \
  -configuration "${CONFIG}" \
  -sdk iphonesimulator \
  -destination "id=${BOOTED}" \
  -derivedDataPath "${DERIVED}" \
  build

APP_PATH="$(find "${DERIVED}/Build/Products" -maxdepth 2 -name '*.app' | head -1)"
[ -n "${APP_PATH}" ] || { echo "!! No .app built under ${DERIVED}/Build/Products" >&2; exit 1; }

echo ">> Installing ${APP_PATH} on simulator ${BOOTED}"
xcrun simctl install "${BOOTED}" "${APP_PATH}"
echo ">> Launching ${BUNDLE_ID}"
xcrun simctl launch "${BOOTED}" "${BUNDLE_ID}"
echo ">> Done (simulator)."
