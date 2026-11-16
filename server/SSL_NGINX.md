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

## WebSocket (погашения между сканерами)

Проксируйте тот же префикс, что и API, с upgrade:

```nginx
location /scanner/api/v1/ws/ {
    proxy_pass http://127.0.0.1:8000/api/v1/ws/;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_read_timeout 86400;
}
```

Эндпоинт: `wss://<хост>/scanner/api/v1/ws/concerts/{concertId}`.

## Продление Let's Encrypt

После перевыпуска сертификата обновите в приложении файл `app/src/main/res/raw/kora_api_leaf.cer` (если используете запасной вариант в APK) или оставьте только правильный fullchain на сервере.