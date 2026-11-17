# К mobile — Виртуальный оператор (Android)

## Скачать APK
Готовый установочный файл APK добавлен прямо в репозиторий, чтобы его можно было скачать с GitHub:

- **[apk/k-mobile-debug.apk](apk/k-mobile-debug.apk)** (нажмите на файл в GitHub и выберите **Download raw file** / **Raw**)

## Основные функции
- Авторизация через Google (`Credential Manager` + Firebase Auth) и хранение данных в Cloud Firestore
- Обязательное оформление абонентского договора «К mobile» с электронной подписью
- Виртуальные номера с префиксом `+7 990`
- Тарифы: **«К mobile Старт» (0 ₽ / мес)** и **«К mobile 100» (100 ₽ / мес)**
- Звонки через интернет-сеть с переходом в настоящий вызов Android (`ACTION_CALL`)
- Запрос системных разрешений Android (`CALL_PHONE`, `RECORD_AUDIO`, `READ_PHONE_STATE`, `READ_CONTACTS`, `POST_NOTIFICATIONS`)
