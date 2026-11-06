#!/usr/bin/env bash
# Builds ShadowFist.apk with the Android SDK build-tools directly (no Gradle).
# usage: build.sh <build-dir>
set -euo pipefail
SRC="$(cd "$(dirname "$0")" && pwd)"
OUT="${1:-$SRC/build}"
SDK="${ANDROID_HOME:-$LOCALAPPDATA/Android/Sdk}"
BT="$SDK/build-tools/34.0.0"
JAR="$SDK/platforms/android-34/android.jar"
rm -rf "$OUT"; mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex"

"$BT/aapt2" compile --dir "$SRC/res" -o "$OUT/res.zip"
"$BT/aapt2" link -o "$OUT/base.apk" -I "$JAR" --manifest "$SRC/AndroidManifest.xml" \
  --java "$OUT/gen" --min-sdk-version 24 --target-sdk-version 34 \
  --version-code 1 --version-name 1.0 "$OUT/res.zip"
javac -nowarn -source 8 -target 8 -bootclasspath "$JAR" -d "$OUT/classes" \
  "$SRC/src/com/shadowfist/game/MainActivity.java" "$OUT/gen/com/shadowfist/game/R.java" 2>&1 | grep -v "^warning" || true
java -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 --release --min-api 24 --lib "$JAR" \
  --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')
cp "$OUT/base.apk" "$OUT/unaligned.apk"
# assets added with jar (stored, forward slashes); aapt2 -A writes backslash paths on Windows
jar -u -0 -M -f "$OUT/unaligned.apk" -C "$SRC" assets
(cd "$OUT/dex" && jar -u -0 -M -f ../unaligned.apk classes.dex)
"$BT/zipalign" -f 4 "$OUT/unaligned.apk" "$OUT/aligned.apk"
KS="$SRC/shadowfist.keystore"
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -alias shadowfist -keyalg RSA -keysize 2048 \
  -validity 10000 -storepass shadowfist -keypass shadowfist -dname "CN=Shadow Fist" >/dev/null
java -jar "$BT/lib/apksigner.jar" sign --ks "$KS" --ks-pass pass:shadowfist --out "$SRC/ShadowFist.apk" "$OUT/aligned.apk"
java -jar "$BT/lib/apksigner.jar" verify -v "$SRC/ShadowFist.apk" | head -4 && echo "OK: $SRC/ShadowFist.apk"
