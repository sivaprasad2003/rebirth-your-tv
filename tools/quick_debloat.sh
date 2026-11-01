#!/usr/bin/env bash
# Smart TV Auto-Detector: queries the connected TV model over ADB and matches
# it against tested profiles in examples/.
#
# Usage:
#     tools/quick_debloat.sh
#     TV_HOST=192.168.1.50 tools/quick_debloat.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

TV_PY="$SCRIPT_DIR/tv.py"
DISABLE_SH="$SCRIPT_DIR/disable.sh"

echo "Probing connected Smart TV..."
PROBE_OUTPUT=$(python3 "$TV_PY" --probe 2>&1 || true)
echo "$PROBE_OUTPUT"

BRAND=$(python3 "$TV_PY" "getprop ro.product.brand" 2>/dev/null | tr -d '\r\n' | tr '[:upper:]' '[:lower:]' || true)
MODEL=$(python3 "$TV_PY" "getprop ro.product.model" 2>/dev/null | tr -d '\r\n' | tr '[:upper:]' '[:lower:]' || true)
DEVICE=$(python3 "$TV_PY" "getprop ro.product.device" 2>/dev/null | tr -d '\r\n' | tr '[:upper:]' '[:lower:]' || true)

echo "Detected: Brand='$BRAND', Model='$MODEL', Device='$DEVICE'"

MATCH=""
# Search examples directories
for dir in "$REPO_DIR"/examples/*/; do
    [ -d "$dir" ] || continue
    base=$(basename "$dir" | tr '[:upper:]' '[:lower:]')
    debloat="$dir/debloat.txt"
    [ -f "$debloat" ] || continue

    # Check folder name match
    if [ -n "$MODEL" ] && echo "$base" | grep -qi "${MODEL// /-}"; then
        MATCH="$debloat"
        break
    fi
    if [ -n "$DEVICE" ] && echo "$base" | grep -qi "$DEVICE"; then
        MATCH="$debloat"
        break
    fi
    
    # Check debloat header line match
    first_line=$(head -n 1 "$debloat" | tr '[:upper:]' '[:lower:]')
    if [ -n "$MODEL" ] && echo "$first_line" | grep -qi "$MODEL"; then
        MATCH="$debloat"
        break
    fi
done

if [ -n "$MATCH" ]; then
    echo "Found matching debloat profile: $MATCH"
    echo "Applying profile..."
    "$DISABLE_SH" "$MATCH"
else
    echo "No exact match found in examples/ for Model: '$MODEL'."
    echo "Available profiles for brand '$BRAND':"
    find "$REPO_DIR/examples" -maxdepth 1 -type d -iname "*$BRAND*" -exec basename {} \;
    echo "To apply manually: tools/disable.sh examples/<model>/debloat.txt"
fi

