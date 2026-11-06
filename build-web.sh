#!/usr/bin/env bash
# Copies the game into web/, which is what gets hosted for the home-screen install.
set -euo pipefail
SRC="$(cd "$(dirname "$0")" && pwd)"
cp "$SRC/assets/index.html" "$SRC/web/index.html"
echo "OK: $SRC/web"
