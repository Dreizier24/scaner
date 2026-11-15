# КОРА Scanner — API (демо-сервер)

FastAPI + SQLite. Совместим с Android-клиентом (`BuildConfig.API_BASE_URL` → `.../api/v1/`).

## Запуск локально

```bash
cd server
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Проверка: http://127.0.0.1:8000/health  
Список концертов: http://127.0.0.1:8000/api/v1/concerts  
Документация: http://127.0.0.1:8000/docs

## Тестовые данные

| ID концерта | Название | Билетов |
|-------------|----------|---------|
| `c-symphony-2026-11-05` | Симфонический оркестр «Волшебный луч» | **6** (в т.ч. 2 Пушкинских) |
| `c-islamey-2026-11-12` | «Исламей» | 2 |
| `c-theatre-2026-11-28` | «Здравствуйте, я ваша тётя!» | 2 |

Штрихкоды для QR (первый концерт):

- `KORA-C1-3-12` — ряд 3, место 12, Партер
- `KORA-C1-3-13` — ряд 3, место 13
- `PUSHKIN-C1-7-08` — Пушкинская карта, ряд 7, место 8
- `4601234567890` — EAN-13 тест

База: `server/data/kora_scanner.db` (создаётся при первом запуске). Удалите файл, чтобы пересоздать seed.

## Подключение телефона

1. В `app/build.gradle.kts` для debug:

   ```kotlin
   buildConfigField("String", "API_BASE_URL", "\"http://ВАШ_IP:8000/api/v1/\"")
   ```

2. Телефон и ПК в одной Wi‑Fi; разрешите входящие на порту 8000 в файрволе.
3. Пересоберите APK.

Для эмулятора Android: `http://10.0.2.2:8000/api/v1/`

## Деплой на сервер

```bash
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

За nginx — проксируйте `/api/v1/` на uvicorn. Для продакшена: systemd + gunicorn/uvicorn workers, HTTPS, бэкап `data/kora_scanner.db`.

## Redis (позже)

Сейчас **revision** и гашения в SQLite. При росте нагрузки можно вынести hot-path в Redis по схеме из `app/../RedisSyncContract` в Android-проекте; этот сервер остаётся источником правды для манифеста.