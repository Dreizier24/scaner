# Cursor-агент: сборка APK на Linux-сервере

Задача: собрать `assembleDebug` (или `assembleRelease` при настроенном keystore) и вызвать `publish-apk.sh`.

Перед работой прочитайте [BUILD_APK.md](./BUILD_APK.md) и [server/CURSOR_AGENT_SERVER.md](../server/CURSOR_AGENT_SERVER.md).

## Предусловия

- Проект: `/opt/scaner` (или `git clone` в этот путь).
- RAM: при `<6 GiB` — создать **4G swap** (см. BUILD_APK).
- `JAVA_HOME` = JDK 17.
- `ANDROID_HOME` с `platforms;android-35`, `build-tools;35.0.0`, `platform-tools`.

## Установка Android SDK (один раз)

```bash
export ANDROID_HOME=/opt/android-sdk
sudo mkdir -p "$ANDROID_HOME/cmdline-tools"
cd /tmp
wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip
unzip -q cmdline-tools.zip -d /tmp/ct
sudo mkdir -p "$ANDROID_HOME/cmdline-tools/latest"
sudo mv /tmp/ct/cmdline-tools/* "$ANDROID_HOME/cmdline-tools/latest/"
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_HOME" \
  "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

`local.properties` в корне проекта (не в git):

```
sdk.dir=/opt/android-sdk
```

В репозитории должны быть `gradlew` и `gradle/wrapper/gradle-wrapper.jar`.

## Сборка

```bash
cd /opt/scaner
git pull origin main
export ANDROID_HOME=/opt/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew assembleDebug --no-daemon --max-workers=1
```

При `Gradle build daemon disappeared` — swap, `--max-workers=1`, `-Dorg.gradle.jvmargs=-Xmx1536m`.

## Публикация

Версии взять из `app/build.gradle.kts` (`versionCode`, `versionName`):

```bash
/opt/scaner/server/scripts/publish-apk.sh \
  /opt/scaner/app/build/outputs/apk/debug/app-debug.apk \
  "$VERSION_CODE" "$VERSION_NAME" "Собрано на сервере"
```

## Проверка

```bash
ls -lh /opt/scaner/server/releases/kora-scanner.apk
curl -fsS https://37.252.21.72.nip.io/scanner/app/update.json | python3 -m json.tool
```

## Не коммитить

- `local.properties`
- `*.apk` (в git игнорируются; на диске лежат в `server/releases/`)