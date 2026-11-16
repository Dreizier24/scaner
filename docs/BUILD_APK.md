# Сборка APK и публикация на сервер (OTA)

Приложение **1.0.5+** при входе запрашивает `APP_UPDATE_URL` (`…/scanner/app/update.json`). Если `versionCode` на сервере больше, чем в установленном APK — диалог **«Обновить»** → скачивание `apkUrl` → системный установщик.

## Версии

В `app/build.gradle.kts`:

- `versionCode` — целое, **строго увеличивать** при каждом релизе.
- `versionName` — строка для людей (например `1.0.6`).
- `API_BASE_URL` / `APP_UPDATE_URL` — прод: `https://37.252.21.72.nip.io/scanner/…`

После сборки **обязательно** опубликовать APK и обновить манифест тем же `versionCode` (см. ниже).

## Вариант A — Android Studio (рекомендуется)

1. Клон: `git clone https://github.com/Dreizier24/scaner.git`
2. Открыть проект, дождаться Gradle Sync (JDK **17**).
3. **Build → Build APK(s)** (Debug) или **Generate Signed Bundle/APK** (Release).
4. APK:
   - Debug: `app/build/outputs/apk/debug/app-debug.apk`
   - Release: `app/build/outputs/apk/release/app-release.apk`
5. Залить на VPS:

```bash
scp app/build/outputs/apk/debug/app-debug.apk root@37.252.21.72:/tmp/kora.apk
ssh root@37.252.21.72 '/opt/scaner/server/scripts/publish-apk.sh /tmp/kora.apk 6 1.0.5 "Описание"'
```

Подставьте актуальные `versionCode`, `versionName`.

## Вариант B — сборка на VPS (`/opt/scaner`)

На маленьком VPS (≈4 ГБ RAM) без swap Gradle часто падает по OOM — **сначала swap**:

```bash
sudo fallocate -l 4G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
```

Один раз (если ещё нет):

```bash
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk wget unzip
# Android SDK — см. docs/CURSOR_AGENT_ANDROID_BUILD.md
```

Сборка:

```bash
cd /opt/scaner
export ANDROID_HOME=/opt/android-sdk   # или ваш путь
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
# local.properties: sdk.dir=... (в git не коммитится)
./gradlew assembleDebug --no-daemon --max-workers=1
```

Публикация:

```bash
/opt/scaner/server/scripts/publish-apk.sh \
  /opt/scaner/app/build/outputs/apk/debug/app-debug.apk \
  6 1.0.5 "Релиз"
```

## Первая установка с OTA

APK с функцией OTA (**1.0.5 / code 6**) один раз нужно поставить вручную:

`https://37.252.21.72.nip.io/scanner/releases/kora-scanner.apk`

(файл появится после `publish-apk.sh`)

Дальнейшие версии — диалог при открытии приложения (Android всё равно попросит подтвердить установку).

## Подпись

Обновление «поверх» работает только при **той же подписи** (тот же debug/release keystore). Для прода заведите release keystore и не теряйте его.

## Чеклист релиза

1. Увеличить `versionCode` / `versionName` в `app/build.gradle.kts`.
2. Собрать APK.
3. `publish-apk.sh` с тем же `versionCode`.
4. `curl https://37.252.21.72.nip.io/scanner/app/update.json`
5. На телефоне со **старой** версией — открыть приложение → «Обновить».

## API без смены APK

Изменения только в `server/` → на телефоне достаточно `git pull` + `./server/deploy.sh` на VPS; новый APK не нужен, если клиент уже умеет вызывать эти эндпоинты.