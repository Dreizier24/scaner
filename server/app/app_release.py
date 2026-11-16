import json
from pathlib import Path

RELEASES_DIR = Path(__file__).resolve().parent.parent / "releases"
UPDATE_JSON = RELEASES_DIR / "update.json"


def read_update_manifest() -> dict:
    if not UPDATE_JSON.is_file():
        return {
            "versionCode": 0,
            "versionName": "0",
            "apkUrl": "",
            "releaseNotes": "",
            "mandatory": False,
        }
    return json.loads(UPDATE_JSON.read_text(encoding="utf-8"))