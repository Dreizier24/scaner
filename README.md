# КОРА — Сканер билетов (Android)

Мобильное приложение для сканирования QR-кодов билетов в стиле [co-ra.ru](https://co-ra.ru/): бордовая палитра, золотые акценты, спокойная «концертная» типографика.

## Стек

- Kotlin, Jetpack Compose, Material 3
- CameraX + ML Kit (QR, Code128, EAN, PDF417 и др.)
- Room — локальный кэш билетов и история сканов
- Retrofit — заготовка под API билетной системы

## Сервер (демо API)

Каталог [`server/`](server/): FastAPI + SQLite, тестовые концерты и билеты. Запуск: `server\run.bat` или см. [server/README.md](server/README.md).

## Запуск приложения

1. Откройте папку проекта в **Android Studio** (Ladybug или новее).
2. Дождитесь синхронизации Gradle и установки SDK.
3. Подключите телефон или эмулятор с камерой → **Run**.

Требования: JDK 17, `compileSdk 35`, `minSdk 26`.

## Формат QR

Поддерживается:

1. **JSON** — поля `ticketId`, `eventTitle`, `venue`, `dateTime`, `seat`, `sector`, `holderName`, `orderId`, `status`
2. **URL** с query-параметрами (`ticket`, `event`, `title`, …)
3. **Произвольный текст** — показывается как есть

Пример JSON для теста (сгенерируйте QR на [qr-code-generator.com](https://www.qr-code-generator.com/)):

```json
{"ticketId":"T-1001","eventTitle":"Государственный симфонический оркестр","venue":"г. Майкоп, ул. Пионерская, 300","dateTime":"05.11.2026 18:30","seat":"12","sector":"Партер","holderName":"Иванов И.И.","status":"valid"}
```

## Концерты, ряд/место, гашение

1. Экран **«Выбор концерта»** — `GET /concerts`.
2. После выбора — **однократная** загрузка манифеста: `GET /concerts/{id}/manifest?since={revision}` (массив `placeId`, ряд, место, штрихкоды, `isPushkin`, `isRedeemed`).
3. Каждое сканирование — **только Room** (`barcode_index` → `place_tickets`), без запроса к API.
4. Гашение: локально `isRedeemed = 1`, очередь `pending_redemptions` → `POST /concerts/{id}/redemptions/sync`.
5. **Пушкинская карта:** при `isPushkin` дополнительно `POST /pushkin/redeem` (когда есть сеть).
6. **Сторонние штрихкоды:** импорт JSON в сканере или `POST /concerts/{id}/barcodes/import`.

Пример импорта:

```json
[
  {"barcode":"4601234567890","row":"3","seat":"12","sector":"Партер","isPushkin":true},
  {"barcode":"T-8801","placeId":"p-8801","row":"5","seat":"1","isPushkin":false}
]
```

## Redis (бэкенд)

Схема для минимизации нагрузки на БД — в `RedisSyncContract.kt`: ревизия манифеста, hash мест, индекс штрихкод → placeId, пачковая синхронизация гашений.

## Цифровой ID (Госуслуги)

Режим **«Цифровой ID»** на сканере → `POST /gosuslugi/digital-id/validate` (прокси на стороне КОРА). Реализация на клиенте: `GosuslugiDigitalIdService.kt`.

## Онлайн / офлайн (архитектура)

| Режим | Поведение |
|--------|-----------|
| **Онлайн** | QR → `POST /tickets/validate` → актуальный статус с сервера → запись в историю |
| **Офлайн** | QR → разбор локально → поиск в Room (`cached_tickets`) → статус из кэша; неизвестные сканы в `pending_validations` |
| **Возврат сети** | Авто-отправка очереди `pending_validations` на сервер |

**Офлайн-пакет:** кнопка «Загрузить офлайн-пакет» вызывает `GET /tickets/offline-pack` и сохраняет билеты смены/мероприятия в Room. Его нужно обновлять перед контролем на площадке без стабильного интернета.

Контракт API описан в `TicketApiService.kt`. Пока backend КОРА не подключён, онлайн-валидация и синхронизация вернут ошибку — приложение продолжит работать с локальным разбором QR.

## Структура модулей

```
app/src/main/java/ru/cora/scanner/
  camera/          — CameraX + ML Kit
  data/            — Repository, NetworkMonitor, Room, Retrofit
  domain/          — модели и парсер QR
  ui/              — экран сканера и тема КОРА
```

## Дальнейшие шаги

- Подключить реальный API билетов КОРА (`BuildConfig.API_BASE_URL`)
- Авторизация контролёра (JWT / PIN)
- Звук и цветовая индикация статуса (допуск / отказ)
- Экспорт отчёта сканов за смену
