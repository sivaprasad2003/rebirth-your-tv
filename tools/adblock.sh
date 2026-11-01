#!/usr/bin/env bash
# System-wide AdGuard Private DNS Switcher for Android TV / Google TV.
#
# Usage:
#     tools/adblock.sh on            enable AdGuard DNS (dns.adguard-dns.com)
#     tools/adblock.sh off           disable private DNS (set to off)
#     tools/adblock.sh status        check current private DNS state
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TV_PY="$SCRIPT_DIR/tv.py"

ACTION="${1:-status}"
DNS_HOST="${2:-dns.adguard-dns.com}"

case "$ACTION" in
    on|enable)
        echo "Enabling Private DNS ($DNS_HOST)..."
        python3 "$TV_PY" "settings put global private_dns_mode hostname"
        python3 "$TV_PY" "settings put global private_dns_specifier $DNS_HOST"
        echo "Private DNS enabled: $DNS_HOST"
        ;;
    off|disable)
        echo "Disabling Private DNS..."
        python3 "$TV_PY" "settings put global private_dns_mode off"
        python3 "$TV_PY" "settings delete global private_dns_specifier" || true
        echo "Private DNS disabled."
        ;;
    status)
        echo "Checking Private DNS status..."
        MODE=$(python3 "$TV_PY" "settings get global private_dns_mode" 2>/dev/null || echo "unknown")
        SPEC=$(python3 "$TV_PY" "settings get global private_dns_specifier" 2>/dev/null || echo "none")
        echo "Mode:      $MODE"
        echo "Specifier: $SPEC"
        ;;
    *)
        echo "Usage: $0 [on|off|status] [custom_dns_hostname]"
        exit 1
        ;;
esac

