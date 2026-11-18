#!/usr/bin/env bash
# Forced command for the GitHub Actions SSH key.
# Client: ssh ... "publish <versionCode> <versionName> <notes>" < app.apk
set -euo pipefail
cmd="${SSH_ORIGINAL_COMMAND:-}"
case "$cmd" in
  publish\ *) ;;
  *)
    echo "denied" >&2
    exit 1
    ;;
esac
# shellcheck disable=SC2086
set -- $cmd
shift
code="${1:?versionCode}"
name="${2:?versionName}"
shift 2
notes="${*:-GitHub Actions}"
tmp="$(mktemp /tmp/kora-apk.XXXXXX.apk)"
trap 'rm -f "$tmp"' EXIT
cat > "$tmp"
exec /opt/scaner/server/scripts/publish-apk.sh "$tmp" "$code" "$name" "$notes"
