#!/usr/bin/env bash
# Build 每日一遍 (release APK) without Gradle: javac + d8 + aapt + zipalign + apksigner.
#
# No secrets in this file. The signing keystore is NOT in the repo — put yours
# at keystore/release.jks (see .gitignore) and give its password via the
# KS_PASS environment variable, or a keystore.properties file next to this script:
#   storePassword=<keystore password>
#   keyPassword=<key password>   (optional, defaults to storePassword)
#
# Toolchain (override via environment when your paths differ):
#   JAVA_HOME, ANDROID_HOME / ANDROID_SDK_ROOT, R8_JAR, KS (keystore path)
set -euo pipefail
PJ="$(cd "$(dirname "$0")" && pwd)"
B="$PJ/build"

JB="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-arm64}/bin"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-arm64}"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/lib/android-sdk}}"
BT="$SDK/build-tools/34.0.0"
export PATH="$JB:$BT:$PATH"
AJ="${ANDROID_JAR:-$SDK/platforms/android-34/android.jar}"  # API 34+: splash screen attrs (windowSplashScreen*) need API 31+
R8="${R8_JAR:-/root/apkbuild/dl/r8.jar}"
KS="${KS:-$PJ/keystore/release.jks}"
KSALIAS="${KS_ALIAS:-lengroo}"

KSPASS="${KS_PASS:-}"
if [ -z "$KSPASS" ] && [ -f "$PJ/keystore.properties" ]; then
  KSPASS="$(grep -E '^storePassword=' "$PJ/keystore.properties" | cut -d= -f2-)"
fi
KEYPASS="${KEY_PASS:-$KSPASS}"
if [ -z "$KSPASS" ]; then
  echo "error: signing password missing — set KS_PASS or create keystore.properties (storePassword=...)" >&2
  exit 1
fi

rm -rf "$B" && mkdir -p "$B/classes" "$B/dex"

echo "[1/6] javac"
find "$PJ/app/src/main/java" -name '*.java' > "$B/srcs.txt"
"$JB/javac" -nowarn -source 8 -target 8 -bootclasspath "$AJ" \
  -encoding UTF-8 -d "$B/classes" @"$B/srcs.txt" 2>&1 | grep -v 'bootstrap class path' || true

echo "[2/6] d8"
if command -v d8 >/dev/null 2>&1; then
  d8 --min-api 21 --lib "$AJ" --release \
    --output "$B/dex" $(find "$B/classes" -name '*.class') 2>&1 | tail -3
elif [ -f "$R8" ]; then
  "$JB/java" -cp "$R8" com.android.tools.r8.D8 \
    --min-api 21 --lib "$AJ" --release \
    --output "$B/dex" $(find "$B/classes" -name '*.class') 2>&1 | tail -3
else
  echo "error: no d8 found (install Android SDK build-tools or set R8_JAR)" >&2
  exit 1
fi

echo "[3/6] aapt package"
aapt package -f -M "$PJ/app/src/main/AndroidManifest.xml" \
  -S "$PJ/app/src/main/res" \
  -A "$PJ/app/src/main/assets" \
  -I "$AJ" -F "$B/unsigned.apk"

echo "[4/6] add classes.dex"
cd "$B/dex" && aapt add "$B/unsigned.apk" classes.dex >/dev/null
cd "$B"

echo "[5/6] zipalign"
zipalign -f -p 4 "$B/unsigned.apk" "$B/aligned.apk"

echo "[6/6] sign (release)"
mkdir -p "$PJ/keystore"
if [ ! -f "$KS" ]; then
  "$JB/keytool" -genkeypair -keystore "$KS" -alias "$KSALIAS" \
    -keyalg RSA -keysize 2048 -validity 20000 \
    -storepass "$KSPASS" -keypass "$KEYPASS" \
    -dname "CN=lengroo, OU=snfx, O=lengroo, L=Shanghai, C=CN" >/dev/null 2>&1
fi
apksigner sign --ks "$KS" --ks-key-alias "$KSALIAS" \
  --ks-pass pass:$KSPASS --key-pass pass:$KEYPASS \
  --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true \
  --out "$B/每日一遍.apk" "$B/aligned.apk"
apksigner verify -v --print-certs "$B/每日一遍.apk" | head -6
ls -la "$B"/*.apk
