#!/usr/bin/env bash
# Run RepoLogicTest: QuoteRepo 纯逻辑测试，不依赖 Android 运行时，直接 java 跑。
# Toolchain (override via environment when your paths differ):
#   JAVA_HOME, ANDROID_HOME / ANDROID_SDK_ROOT, ANDROID_JAR
set -euo pipefail
PJ="$(cd "$(dirname "$0")/.." && pwd)"
JB="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-arm64}/bin"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/lib/android-sdk}}"
AJ="${ANDROID_JAR:-$SDK/platforms/android-34/android.jar}"
T="$PJ/build-test"
rm -rf "$T" && mkdir -p "$T/classes"
"$JB/javac" -nowarn -source 8 -target 8 -bootclasspath "$AJ" -encoding UTF-8 \
  -d "$T/classes" \
  $(find "$PJ/app/src/main/java" -name '*.java') \
  $(find "$PJ/tools/tests" -name '*.java')
"$JB/java" -cp "$T/classes:$AJ" RepoLogicTest
