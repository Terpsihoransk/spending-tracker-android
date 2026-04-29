# Инструкции для ИИ-агентов (AGENTS.md)

Этот файл содержит контекст и правила для ИИ-агентов, работающих над проектом **Spending Tracker Android**.

## Обзор проекта
- **Цель**: Android-приложение для учета расходов с синхронизацией через Backend в Google Sheets.
- **Архитектура**: Clean Architecture (Domain -> Data -> Presentation).
- **Стек**: Kotlin 2.3.21, Jetpack Compose, Room, Ktor, Koin.

## Правила разработки

### 1. Архитектурные слои
- **Domain**: Только чистый Kotlin. Никаких зависимостей от Android, Room или Ktor. Модели данных, интерфейсы репозиториев и UseCases.
- **Data**: Реализация репозиториев. Здесь живут Room Entities, DAOs, Ktor API сервисы и DTOs.
- **Presentation**: Jetpack Compose экраны и ViewModels. Используйте StateFlow для передачи состояния в UI.
- **DI**: Все зависимости должны регистрироваться в `AppModule.kt` через Koin.

### 2. Работа с данными
- Используйте паттерн **Offline-first**: данные сначала запрашиваются из сети, сохраняются в Room, и UI всегда подписывается на Flow из Room.
- Для преобразования данных между слоями используйте мапперы в `util/Mappers.kt`.

### 3. Конфигурация Gradle
- **Важно**: Проект использует AGP 9.2.0 и Kotlin 2.3.21.
- Не используйте устаревший `kotlinOptions`. Вместо него используйте `compilerOptions` (если поддерживается) или стандартные `compileOptions`.
- Все версии библиотек управляются через `gradle/libs.versions.toml`.

### 4. Чек-лист и планы
- Перед началом работы всегда сверяйтесь с `plans/checklist.md`.
- Детальное описание этапов находится в `plans/detailed_android_plan.md`.

### 5. Совместимость
- В gradle.properties установлен флаг android.disallowKotlinSourceSets=false
  (необходим для совместимости KSP с AGP 9.x)

## Полезные команды
- Сборка: `gradle assembleDebug`
- Тесты: `gradle test`

## Контекст Backend
- Base URL: `http://10.0.2.2:8081/api/v1/` (для эмулятора).
- Порт: 8081 (порт 8080 занят Postgres).
- Сущности: User, Category, SubCategory, Spending.
- Авторизация: В MVP1 используется упрощенная передача `userId` или `email` в заголовках/параметрах.
