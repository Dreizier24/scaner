package ru.cora.scanner.data.sync

/**
 * Соглашение клиент ↔ сервер при использовании Redis на бэкенде.
 *
 * Сервер (рекомендуемая схема):
 * - `concert:{id}:rev` — монотонная ревизия манифеста (INCR при изменении мест/гашений).
 * - `concert:{id}:places` — HASH placeId → JSON (ряд, место, штрихкоды, pushkin, redeemed).
 * - `concert:{id}:barcode:{code}` — STRING → placeId (O(1) поиск при скане).
 * - Очередь гашений: `concert:{id}:redemptions:pending` (LIST) — воркер пишет в БД.
 *
 * Клиент минимизирует обращения к БД:
 * 1. После выбора концерта — один запрос `GET /concerts/{id}/manifest?since={localRev}`.
 * 2. Если `revision` не изменилась — 304, манифест не качаем.
 * 3. Скан — только локальный Room + запись в `pending_redemptions`.
 * 4. Периодически / при появлении сети — `POST /concerts/{id}/redemptions/sync` пачкой.
 * 5. Опционально `GET /concerts/{id}/sync-state` — сверка rev с Redis без полного манифеста.
 */
object RedisSyncContract
