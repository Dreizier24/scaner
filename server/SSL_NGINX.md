# SSL на сервере (если на Android «ошибка соединения»)

На ПК сайт часто открывается, а на Android — нет: nginx отдаёт **только leaf-сертификат** без цепочки Let's Encrypt.

## Исправление (nginx)

```nginx
ssl_certificate     /etc/letsencrypt/live/ВАШ_ДОМЕН/fullchain.pem;
ssl_certificate_key /etc/letsencrypt/live/ВАШ_ДОМЕН/privkey.pem;

ssl_protocols TLSv1.2 TLSv1.3;
ssl_prefer_server_ciphers off;
```

Не используйте `cert.pem` вместо `fullchain.pem`.

Проверка:

```bash
sudo nginx -t && sudo systemctl reload nginx
```

Проверка с телефона после правки: приложение **1.0.2+** или любой браузер на Android.

## Продление Let's Encrypt

После перевыпуска сертификата обновите в приложении файл `app/src/main/res/raw/kora_api_leaf.cer` (если используете запасной вариант в APK) или оставьте только правильный fullchain на сервере.