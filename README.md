# Spending Tracker Android

Приложение для учёта личных расходов с синхронизацией в Google Sheets.

**Версия**: v0.0.1 (MVP1)

## Архитектура
Приложение построено на принципах **Clean Architecture**:
- **Domain**: Бизнес-логика и модели.
- **Data**: Room (локальный кэш) + Ktor (REST API).
- **Presentation**: Jetpack Compose + MVVM.
- **DI**: Koin.

## Технологии
- **Kotlin**: 2.3.21
- **JDK**: 17
- **Android SDK**: 37
- **Jetpack Compose**: BOM 2026.04.01
- **Room**: 2.8.4
- **Ktor**: 3.4.3
- **Koin**: 4.2.1

## Модули проекта
| Модуль | Статус | Описание |
|--------|--------|----------|
| `backend/` | ✅ Готов | Java Spring Boot REST API |
| `app/` | 📋 В разработке | Kotlin Android приложение |

## Документация
- [Детальный план разработки](plans/detailed_android_plan.md)
- [Чек-лист реализации](plans/checklist.md)
- [Инструкции для агентов](AGENTS.md)
