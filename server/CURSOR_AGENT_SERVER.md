# Инструкция для Cursor-агента (только сервер)

Не собирать Android и не трогать `app/`, если пользователь явно не просит сборку APK (см. [docs/BUILD_APK.md](../docs/BUILD_APK.md)).

## Контекст

| Параметр | Значение |
|----------|----------|
| Репозиторий | `https://github.com/Dreizier24/scaner` |
| Каталог на VPS | `/opt/scaner` |
| API (uvicorn) | `127.0.0.1:8020`, PM2: `scanner-server` |
| Публичный префикс | `https://37.252.21.72.nip.io/scanner/` |
| nginx-конфиг | `/etc/nginx/sites-available/kopa-bot` |

## Обновление только API (типичная задача)

```bash
cd /opt/scaner
git pull origin main
./server/deploy.sh
curl -fsS https://37.252.21.72.nip.io/scanner/health
curl -fsS https://37.252.21.72.nip.io/scanner/app/update.json
```

`deploy.sh`: `git pull` → venv → `pip install -r requirements.txt` → `pm2 startOrReload server/ecosystem.config.cjs`.

## OTA (манифест + APK)

- Манифест: FastAPI `GET /app/update.json` (читает `server/releases/update.json`).
- APK: статика nginx `location /scanner/releases/` → `/opt/scaner/server/releases/kora-scanner.apk`.
- После сборки APK (на ПК или на VPS — см. BUILD_APK):

```bash
/opt/scaner/server/scripts/publish-apk.sh \
  /path/to/app-debug.apk \
  <versionCode> \
  <versionName> \
  "Краткое описание релиза"
```

Скрипт копирует APK в `releases/` и перезаписывает `update.json` (поле `apkUrl` уже с nip.io).

Проверка:

```bash
curl -fsS https://37.252.21.72.nip.io/scanner/app/update.json
curl -fsI https://37.252.21.72.nip.io/scanner/releases/kora-scanner.apk | head -5
```

## nginx (не ломать)

Должны быть **до** общего `location /scanner/`:

1. `location /scanner/releases/` — `alias /opt/scaner/server/releases/;`
2. `location /scanner/api/v1/ws/` — WebSocket upgrade → `8020` (см. `server/SSL_NGINX.md`)

SSL: только `fullchain.pem`, не `cert.pem`.

После правок: `nginx -t && systemctl reload nginx`.

## WebSocket

`wss://37.252.21.72.nip.io/scanner/api/v1/ws/concerts/{concertId}`

## База SQLite

`server/data/kora_scanner.db` — не удалять без запроса. При первом старте — seed + `c-loadtest-2000`.

## Чего не делать

- Не менять MAX/другие проекты на этом хосте.
- Не коммитить `.env`, ключи, бинарный APK в git (APK только в `server/releases/` на диске).
- Не пушить в GitHub без явной просьбы пользователя (кроме задачи «залить на GitHub»).

## Связанные файлы

- `server/ecosystem.config.cjs` — PM2
- `server/SSL_NGINX.md` — SSL и WebSocket
- `docs/BUILD_APK.md` — сборка APK и цепочка релиза
- `docs/CURSOR_AGENT_ANDROID_BUILD.md` — агенту, который собирает APK на сервере