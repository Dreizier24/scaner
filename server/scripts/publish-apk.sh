#!/usr/bin/env bash
# Кладёт собранный APK в releases/ и поднимает versionCode в update.json.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK_SRC="${1:-}"
VERSION_CODE="${2:-}"
VERSION_NAME="${3:-}"
NOTES="${4:-Обновление приложения}"

if [[ -z "$APK_SRC" || -z "$VERSION_CODE" || -z "$VERSION_NAME" ]]; then
  echo "Usage: $0 <path-to.apk> <versionCode> <versionName> [releaseNotes]" >&2
  exit 1
fi
if [[ ! -f "$APK_SRC" ]]; then
  echo "APK not found: $APK_SRC" >&2
  exit 1
fi

mkdir -p "$ROOT/releases"
cp -f "$APK_SRC" "$ROOT/releases/kora-scanner.apk"
chmod 644 "$ROOT/releases/kora-scanner.apk"

python3 - <<PY
import json
from pathlib import Path
p = Path("$ROOT/releases/update.json")
data = {
    "versionCode": int("$VERSION_CODE"),
    "versionName": "$VERSION_NAME",
    "apkUrl": "https://37.252.21.72.nip.io/scanner/releases/kora-scanner.apk",
    "releaseNotes": """$NOTES""",
    "mandatory": False,
}
p.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("Wrote", p)
PY

echo "Published: $ROOT/releases/kora-scanner.apk"