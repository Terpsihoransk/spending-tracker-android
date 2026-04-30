# Диагностика и план починки: HttpRequestTimeoutException при обращении к бэкенду из Android-приложения

> **Обновлено 2026-04-30 01:36 MSK** — добавлена информация о восстановлении `adb reverse` туннеля.

---

## ⚠️ ВАЖНО: `adb reverse` сбрасывается после перезапуска

**Симптом:** `ECONNREFUSED (Connection refused)` несмотря на то, что код исправлен.

**Причина:** `adb reverse tcp:8081 tcp:8081` — туннель **не сохраняется** между сессиями ADB/эмулятора.

**Лечение:** После каждого перезапуска ADB или эмулятора выполнить:

```cmd
"C:\Users\{$USERNAME}\AppData\Local\Android\Sdk\platform-tools\adb.exe" reverse tcp:8081 tcp:8081
```

Проверить статус:
```cmd
"C:\Users\{$USERNAME}\AppData\Local\Android\Sdk\platform-tools\adb.exe" reverse --list
```
Ожидаемый вывод: `host-XX tcp:8081 tcp:8081`

---
