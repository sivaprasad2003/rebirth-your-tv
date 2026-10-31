#!/usr/bin/env bash
# Full system state backup of the TV before debloating: packages, properties, settings.
# Saves package states (installed, enabled, disabled), system properties, and Android settings
# into backup/<model>_<timestamp>/
#
# Usage: TV_HOST=192.168.1.50 tools/backup.sh
set -euo pipefail

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TV_PY="$REPO/tools/tv.py"

if [[ -z "${TV_HOST:-}" ]]; then
    echo "Set TV_HOST to your TV's IP address: TV_HOST=192.168.1.50 $0" >&2
    exit 1
fi

MODEL="$("$TV_PY" 'getprop ro.product.model' 2>/dev/null | tr -d '\r\n' || echo 'UnknownTV')"
DEVICE="$("$TV_PY" 'getprop ro.product.device' 2>/dev/null | tr -d '\r\n' || echo 'unknown')"
VERSION="$("$TV_PY" 'getprop ro.build.version.release' 2>/dev/null | tr -d '\r\n' || echo 'unknown')"
SDK="$("$TV_PY" 'getprop ro.build.version.sdk' 2>/dev/null | tr -d '\r\n' || echo 'unknown')"

CLEAN_MODEL="$(echo "$MODEL" | sed -E 's/[^a-zA-Z0-9_-]/_/g')"
TIMESTAMP="$(date +'%Y%m%d_%H%M%S')"
BACKUP_DIR="$REPO/backup/${CLEAN_MODEL}_${TIMESTAMP}"
mkdir -p "$BACKUP_DIR"

echo "Creating backup in: $BACKUP_DIR"

"$TV_PY" 'pm list packages -u' > "$BACKUP_DIR/packages_all.txt"
"$TV_PY" 'pm list packages -d' > "$BACKUP_DIR/packages_disabled.txt"
"$TV_PY" 'pm list packages -e' > "$BACKUP_DIR/packages_enabled.txt"
"$TV_PY" 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME' > "$BACKUP_DIR/current_home.txt" || true
"$TV_PY" 'getprop' > "$BACKUP_DIR/properties.txt"
"$TV_PY" 'settings list global' > "$BACKUP_DIR/settings_global.txt" 2>/dev/null || true
"$TV_PY" 'settings list secure' > "$BACKUP_DIR/settings_secure.txt" 2>/dev/null || true
"$TV_PY" 'settings list system' > "$BACKUP_DIR/settings_system.txt" 2>/dev/null || true

TOTAL_COUNT=$(wc -l < "$BACKUP_DIR/packages_all.txt" | tr -d ' ')
DISABLED_COUNT=$(wc -l < "$BACKUP_DIR/packages_disabled.txt" | tr -d ' ')

cat <<EOF
============================================================
 RebirthYourTV Backup Engine: Completed Successfully!
============================================================
 TV Model:       $MODEL ($DEVICE)
 Android OS:     $VERSION (SDK $SDK)
 Total Packages: $TOTAL_COUNT
 Disabled Apps:  $DISABLED_COUNT
 Backup Folder:  $BACKUP_DIR
============================================================
 To restore packages anytime, run: tools/restore.sh <debloat.txt>
============================================================
EOF

