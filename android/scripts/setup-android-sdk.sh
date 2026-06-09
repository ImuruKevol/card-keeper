#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK_DIR="${ANDROID_SDK_ROOT:-$ROOT_DIR/.android-sdk}"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-14742923_latest.zip"
TMP_DIR="$(mktemp -d)"

cleanup() {
    rm -rf "$TMP_DIR"
}
trap cleanup EXIT

mkdir -p "$SDK_DIR/cmdline-tools"

if [ ! -x "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" ]; then
    echo "Downloading Android command-line tools..."
    wget -q --show-progress "$CMDLINE_TOOLS_URL" -O "$TMP_DIR/cmdline-tools.zip"
    unzip -q "$TMP_DIR/cmdline-tools.zip" -d "$TMP_DIR"
    rm -rf "$SDK_DIR/cmdline-tools/latest"
    mkdir -p "$SDK_DIR/cmdline-tools/latest"
    mv "$TMP_DIR/cmdline-tools"/* "$SDK_DIR/cmdline-tools/latest/"
fi

SDKMANAGER="$SDK_DIR/cmdline-tools/latest/bin/sdkmanager"

echo "Accepting Android SDK licenses..."
set +o pipefail
yes | "$SDKMANAGER" --sdk_root="$SDK_DIR" --licenses >/dev/null
set -o pipefail

echo "Installing Android SDK packages..."
"$SDKMANAGER" --sdk_root="$SDK_DIR" \
    "platform-tools" \
    "platforms;android-36" \
    "build-tools;36.0.0"

cat > "$ROOT_DIR/local.properties" <<PROPS
sdk.dir=$SDK_DIR
PROPS

echo "Android SDK is ready at $SDK_DIR"
echo "local.properties updated."
