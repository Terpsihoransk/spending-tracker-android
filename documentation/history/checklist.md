# Чек-лист реализации Spending Tracker Android

## Этап 1: Базовая конфигурация и зависимости
- [x] Обновить `libs.versions.toml` (Koin, Ktor, Room, Serialization)
- [x] Настроить `app/build.gradle.kts` (JVM 17, плагины)
- [x] Добавить разрешение на Интернет в `AndroidManifest.xml`
- [x] Синхронизировать Gradle и проверить отсутствие ошибок

## Этап 2: Структура проекта и DI
- [x] Создать дерево пакетов (`di`, `data`, `domain`, `presentation`, `util`)
- [x] Создать `SpendingTrackerApp.kt`
- [x] Инициализировать Koin в Application классе

## Этап 3: Domain слой
- [x] Создать модели: `User`, `Category`, `SubCategory`, `Spending`
- [x] Создать интерфейсы репозиториев (Flow-first API)
- [x] Реализовать UseCases (Spending, Category, SubCategory, User)

## Этап 4: Data слой (Room)
- [x] Создать Room Entities
- [x] Создать DAOs (+ транзакционные `replaceXxx` для clean-sync)
- [x] Создать `AppDatabase`
- [x] Добавить `@Index` на FK-колонки (`userId`, `categoryId`, `subCategoryId`)

## Этап 5: Data слой (Ktor) — синхронизовано со спекой бэка ✅
- [x] Настроить `HttpClient` (+ Logging + expectSuccess=true)
- [x] Создать DTOs по спеке бэка (UserRequest/Response, CategoryRequest/Response, SubCategoryRequest/Response, SpendingRequest/Response)
- [x] Реализовать API сервисы: `SpendingApi`, `CategoryApi`, `SubCategoryApi`, `UserApi` — полный CRUD
- [x] Передавать `X-User-Email` заголовок во всех запросах (кроме POST `/user`)
- [x] Добавить Ktor `Logging` plugin для отладки
- [x] Обработать 404 в `UserApi.getUserByEmail` через `ClientRequestException`
- [x] `POST /spending` не передаёт `date` (сервер проставляет сам), `amount` в ответе — строка (BigDecimal)
- [x] Эндпоинты соответствуют спеке: `/user`, `/categories`, `/subcategories`, `/spending`

## Этап 6: Репозитории и Мапперы
- [x] Создать мапперы (DTO <-> Domain <-> Entity)
- [x] Реализовать `SpendingRepositoryImpl` с полным CRUD (add/update/delete + refresh)
- [x] Реализовать `CategoryRepositoryImpl` с полным CRUD для категорий и подкатегорий
- [x] Реализовать `UserRepositoryImpl` (syncUser автоматически создаёт пользователя через POST `/user`, если его нет)
- [x] Перевести весь проект на identity по `userEmail` вместо `userId` (соответствует спеке)

## Этап 6.5: Рефакторинг (ВЫПОЛНЕН ✅)
- [x] Перевести репозитории на Flow-first API (`observeX` + `refreshX`)
  - [x] `SpendingRepository`: `observeSpendings` + `refreshSpendings` + add/delete
  - [x] `CategoryRepository`: observe/refresh для категорий и подкатегорий
  - [x] `UserRepository`: `observeCurrentUser` + `syncUser` + `clearUser`
- [x] Обновить UseCases под новый API (Observe*, Refresh*)
- [x] Привести error-handling к единому стилю (везде `runCatching` + `onFailure { Log.w }`)
- [x] Удалить устаревший `BakingViewModel.kt` (отсутствовал)
- [x] Вынести парсинг `LocalDate` в `LocalDateSerializer` + утилиту
- [x] Проверка: `gradle compileDebugKotlin` — BUILD SUCCESSFUL ✅

## Этап 7: Presentation слой (UI)

> Референс дизайна: [`Spending Tracker - UI Mockup.html`](../../plans/Spending%20Tracker%20-%20UI%20Mockup.html) + [`interfase.md`](interfase.md:1). Тёмная тема, фиолетово-индиго.

### 7.1 Тема (Material 3, Dark-first) ✅
- [x] `presentation/theme/Color.kt` — токены палитры (`#0a0a14`, `#7c3aed`, `#c084fc`, `#4f46e5`, `#9333ea`, `#e8e0ff`, `#9080c4`, `#27234a`, `#3d3268` и др.)
- [x] `presentation/theme/Type.kt` — Typography (SansSerif; Inter — опционально в будущем)
- [x] `presentation/theme/Shape.kt` — скругления (карточки 12dp, small 10dp, extraLarge 20dp)
- [x] `presentation/theme/Theme.kt` — `SpendingTrackerTheme`, только Dark, без Dynamic Color
- [ ] Добавить шрифт Inter в `res/font/` (опционально, сейчас используется `FontFamily.SansSerif`)
- [x] Удалена старая тема `ui/theme/`

### 7.2 Навигация (Bottom Navigation, 4 вкладки) ✅
- [x] `presentation/navigation/Destination.kt` — sealed class: `Spendings / Categories / Summary / Profile / EmailEntry`
- [x] `presentation/navigation/AppNavHost.kt` — `NavHost` с 5 экранами, условный стартовый экран
- [x] `presentation/navigation/BottomBar.kt` — `NavigationBar` с иконками (Receipt, Category, PieChart, Person)
- [x] Обновить `MainActivity` — `Scaffold { bottomBar + NavHost }`

### 7.3 Переиспользуемые компоненты (`presentation/components/`) ✅
- [x] `SpendingCard` — карточка транзакции (индикатор цвета / Категория / Подкатегория / Комментарий / Дата / Сумма)
- [x] `CategoryCard` — раскрывающаяся карточка с chevron и списком подкатегорий
- [x] `StatCard` + `StatRow` — блоки статистики (для экрана Сводка)
- [x] `PeriodFilterChips` — `FilterChip`'ы для выбора периода (Сегодня/Неделя/Месяц/Всё)
- [x] `TotalBar` — нижняя плашка «Итого за период»
- [x] `AddSpendingFab` — круглый FAB с primary-цветом
- [x] `ExposedDropdownMenu` для категорий и подкатегорий (в `AddSpendingSheet`)
- [x] `PieChartCanvas` + `PieChartLegend` — круговая диаграмма через `Canvas`
- [x] `SectionHeader`, `EmptyState` — мелкие вспомогательные компоненты

### 7.4 Экран «Расходы» (главный) ✅
- [x] `SpendingsViewModel` — `observeSpendings` + фильтр периода + расчёт total + error
- [x] `SpendingsScreen` — topbar + `PeriodFilterChips` + `LazyColumn` из `SpendingCard` + `TotalBar` + FAB
- [x] Delete по долгому нажатию на карточку (onLongClick)
- [x] `AddSpendingSheet` (ModalBottomSheet) — поля: Сумма, Категория ▾, Подкатегория ▾, Комментарий, Дата
- [x] `AddSpendingViewModel` — валидация + `AddSpendingUseCase` / `UpdateSpendingUseCase` (режим Add/Edit)
- [x] Экран редактирования расхода по нажатию: переиспользует `AddSpendingSheet(spendingId = id)` + предзагрузка из кэша

### 7.5 Экран «Категории» ✅
- [x] `CategoriesViewModel` — `observeCategories` + `observeSubCategories` через `toggle()`
- [x] `CategoriesViewModel` — методы `onAddCategory/onUpdateCategory/onDeleteCategory` + аналогичные для SubCategory
- [x] `CategoriesScreen` — раскрывающиеся карточки с `AnimatedVisibility`, FAB для добавления
- [x] Диалоги `NameDialog` / `ConfirmDialog` для редактирования/удаления категорий и подкатегорий
- [x] Иконки Edit / Delete на каждой карточке + кнопка «Добавить подкатегорию» внутри раскрытой карточки
- [x] Полная поддержка CRUD для категорий и подкатегорий через бэкенд

### 7.6 Экран «Сводка» ✅
- [x] `SummaryViewModel` — агрегации по месяцам (последние 6) и категориям поверх `observeSpendings`
- [x] `SummaryScreen` — `StatsRow` + таблица по месяцам + таблица «Категория × Месяц» + `PieChartCanvas` + легенда

### 7.7 Экран «Профиль» ✅
- [x] `ProfileViewModel` — `observeCurrentUser`, `clearUser` (logout + очистка DataStore)
- [x] `ProfileScreen` — аватар-градиент + email + ссылка на Google Sheet + кнопка logout
- [x] Открытие Google-таблицы в браузере (Intent `ACTION_VIEW`)

### 7.8 Сессия и хранение настроек ✅
- [x] Добавить зависимость `androidx.datastore:datastore-preferences`
- [x] `SessionManager` (DataStore) — хранит текущий email пользователя
- [x] `EmailEntryScreen` + `EmailEntryViewModel` — экран ввода email при первом запуске

### 7.9 DI ✅
- [x] Добавить `koin-androidx-compose` (уже было), `koin-core` viewModelOf
- [x] Зарегистрировать все ViewModels в `AppModule` через `viewModelOf(::...)`

## Этап 7.10 Синхронизация со спекой бэка (API_SPECIFICATION.md) ✅
- [x] Сверить эндпоинты с `.plans/API_SPECIFICATION.md` — всё соответствует
- [x] Аутентификация через заголовок `X-User-Email` (бэк не использует `userId` в API)
- [x] Domain: `Category.userEmail`, `Spending.userEmail` + `categoryName`/`subCategoryName` из ответа
- [x] Entity: `CategoryEntity.userEmail`, `SpendingEntity.userEmail`+`categoryName`+`subCategoryName`
- [x] DAO: запросы по `userEmail`, CRUD-методы (`upsertCategory`, `deleteCategory`, и т.д.)
- [x] Room v2, `fallbackToDestructiveMigration(true)` на время MVP
- [x] `POST /spending` без поля `date` (ставит сервер), `amount` в ответе парсится из строки
- [x] `GET /user` возвращает список — `UserApi.getUserByEmail` ищет по email локально
- [x] `syncUser` автоматически создаёт пользователя (POST `/user`), если его нет
- [x] Добавлен `SubCategoryApi` + CRUD в `CategoryRepository` + отдельные UseCases

## Этап 8: Верификация
- [x] Unit-тесты UseCases (Spending, Category, SubCategory, User) через fake-репозитории
      ([`app/src/test/java/.../domain/usecase/SpendingUseCasesTest.kt`](../../app/src/test/java/spending/tracker/android/domain/usecase/SpendingUseCasesTest.kt),
      `CategoryUseCasesTest.kt`, `UserUseCasesTest.kt`, fakes в `fakes/FakeRepositories.kt`) — Turbine + coroutines-test
- [x] Интеграционные тесты Room DAO (in-memory DB через Robolectric) — `DaoTest.kt`,
      покрывают `UserDao`, `CategoryDao`, `SpendingDao` (observe, replace, filter by email/categoryId, ordering)
- [x] `gradle compileDebugKotlin` — BUILD SUCCESSFUL ✅ (после синхронизации со спекой)
- [x] `gradle assembleDebug` — BUILD SUCCESSFUL ✅
- [x] `gradle testDebugUnitTest` — все тесты зелёные ✅
- [ ] Проверка на эмуляторе с backend на `http://10.0.2.2:8081` (требует запущенного эмулятора и бэкенда)
