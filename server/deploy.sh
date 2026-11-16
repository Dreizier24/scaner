#!/usr/bin/env bash
# Обновление scanner API на сервере (без Android). Запуск из /opt/scaner.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
git pull origin main
cd server
if [[ ! -d .venv ]]; then
  python3 -m venv .venv
fi
.venv/bin/pip install -q -U pip
.venv/bin/pip install -q -r requirements.txt
pm2 startOrReload ecosystem.config.cjs
pm2 save
echo "OK: $(curl -fsS http://127.0.0.1:8020/health)"