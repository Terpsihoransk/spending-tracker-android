# План разработки Android приложения Spending Tracker (MVP1)

## 1. Обзор архитектуры
Приложение строится на принципах **Clean Architecture** с разделением на слои:
- **Domain**: Бизнес-логика (модели, UseCases, интерфейсы репозиториев). Не зависит от библиотек.
- **Data**: Реализация данных (Room для локального кэша, Ktor для API, реализации репозиториев).
- **Presentation**: UI на Jetpack Compose и ViewModels.
- **DI**: Внедрение зависимостей через Koin.

## 2. Технологический стек (Актуальные версии)
| Компонент | Версия | Примечание |
|-----------|--------|------------|
| Kotlin | 2.3.21 | |
| Java / JVM Target | 17 | Android стандарт |
| AGP | 9.2.0 | |
| Compose BOM | 2026.04.01 | |
| Room | 2.8.4 | |
| Koin | 4.2.1 | |
| Ktor | 3.4.3 | |
| Navigation Compose | 2.9.8 | |
| KSP | 2.3.2 | |

---

## 3. Модель данных и API (на основе Backend)

### Сущности (Entities)
- **User**: `id`, `email`, `googleSheetsId`.
- **Category**: `id`, `name`, `userId`.
- **SubCategory**: `id`, `name`, `categoryId`.
- **Spending**: `id`, `amount` (BigDecimal/Double), `categoryId`, `subCategoryId`, `date` (LocalDate), `description`, `userId`.

### API Эндпоинты (Base URL: `/api/v1`)
- Пользователи: `/users`
- Категории: `/categories`
- Подкатегории: `/subcategories`
- Расходы: `/spending`

---

## 4. Поэтапный план реализации

### Этап 1: Базовая конфигурация и зависимости
1. Обновить `libs.versions.toml` актуальными версиями библиотек.
2. Настроить `app/build.gradle.kts`:
    - Установить `jvmTarget = "17"`.
    - Подключить плагины (KSP, Serialization, Compose).
    - Добавить зависимости для Koin, Ktor, Room.
3. Добавить `<uses-permission android:name="android.permission.INTERNET" />` в `AndroidManifest.xml`.

### Этап 2: Структура проекта и DI
1. Создать дерево пакетов: `di`, `data`, `domain`, `presentation`, `util`.
2. Создать класс `SpendingTrackerApp` (наследник `Application`) для инициализации Koin.
3. Описать базовые Koin модули (пока пустые).

### Этап 3: Domain слой (Сердце приложения)
1. Создать Domain модели: `User`, `Category`, `SubCategory`, `Spending`.
2. Описать интерфейсы репозиториев: `UserRepository`, `SpendingRepository`, `CategoryRepository`.
3. Реализовать UseCases:
    - `GetSpendingsUseCase`, `AddSpendingUseCase`.
    - `GetCategoriesUseCase`, `GetSubCategoriesUseCase`.
    - `GetCurrentUserUseCase`.

### Этап 4: Data слой (Локальная БД Room)
1. Создать Entity-классы для Room (соответствующие схеме БД бэкенда).
2. Описать DAO интерфейсы для всех сущностей.
3. Создать класс `AppDatabase`.

### Этап 5: Data слой (Сетевой клиент Ktor)
1. Настроить `HttpClient` с сериализацией JSON и обработкой дат.
2. Создать DTO (Data Transfer Objects) для сетевых запросов, соответствующих API v1.
3. Реализовать API сервисы (`UserApi`, `SpendingApi`, `CategoryApi`, `SubCategoryApi`).
4. **Важно**: Для эмулятора использовать адрес `http://10.0.2.2:8081`.

### Этап 6: Реализация репозиториев (Offline-first)
1. Реализовать репозитории с логикой синхронизации.
2. Логика: Сначала запрос в сеть -> сохранение в БД -> отображение из БД.
3. Добавить Mappers для преобразования между DTO, Entity и Domain моделями.

### Этап 7: Presentation слой (UI)

**Источники дизайна:** [`plans/Spending Tracker - UI Mockup.html`](plans/Spending%20Tracker%20-%20UI%20Mockup.html) + [`plans/interfase.md`](plans/interfase.md:1).

#### 7.1 Тема оформления (Dark-first, фиолетово-индиго)
Пакет: `presentation/theme/`

Палитра (Material 3 `ColorScheme`, только Dark):
- `background` = `#0a0a14`, `surface` = `#12121f`, `surfaceVariant` = `#1a1730`, `surfaceContainer` = `#1a1a2e`
- `primary` = `#7c3aed`, `onPrimary` = `#ffffff`
- `primaryContainer` / accent text = `#c084fc`
- `secondary` = `#4f46e5`, `tertiary` = `#9333ea`
- `onBackground` / `onSurface` = `#e8e0ff`, `onSurfaceVariant` = `#9080c4`
- `outline` = `#27234a`, `outlineVariant` = `#3d3268`
- muted text = `#4e4670` / `#5a5278`

Файлы:
- `Color.kt` — токены палитры (`val PurplePrimary = Color(0xFF7C3AED)` и т. д.).
- `Type.kt` — `Typography` на базе Inter (добавить в `res/font/` либо использовать `FontFamily.Default` с корректными весами 400/500/600).
- `Theme.kt` — `SpendingTrackerTheme { ... }`, форсить тёмную схему (Dynamic Color отключить).
- `Shapes.kt` — скругления 12dp для карточек, 10dp для кнопок, 20dp верх у BottomSheet.

#### 7.2 Навигация (Navigation Compose)
Пакет: `presentation/navigation/`
- `Routes.kt` — `sealed class Destination { object Spendings; object Categories; object Summary; object Profile }`.
- `AppNavHost.kt` — `NavHost` с 4 destination'ами.
- `BottomBar.kt` — `NavigationBar` с 4 вкладками: **Расходы / Категории / Сводка / Профиль**. Иконки (Material Icons): `Receipt`, `Category`, `PieChart`, `Person`. Active-цвет `primaryContainer` (#c084fc).
- В `MainActivity`: `Scaffold { bottomBar = BottomBar(); content = AppNavHost() }`.

#### 7.3 Переиспользуемые компоненты
Пакет: `presentation/components/`
- `SpendingCard` — карточка расхода (Категория uppercase, Подкатегория, Комментарий, Дата / Сумма справа).
- `CategoryCard` — раскрывающаяся карточка с `chevron` + кнопка «изм.».
- `StatCard` — блок статистики (Этот месяц / Год).
- `PeriodFilterChip` — кнопка выбора периода в topbar.
- `TotalBar` — нижняя плашка «Итого за месяц».
- `AddSpendingFab` — круглый фиолетовый FAB (`#7c3aed` + glow shadow).
- `CategoryPickerDropdown` / `SubCategoryPickerDropdown` — выпадающие для BottomSheet.
- `PieChartCanvas` — круговая диаграмма через `Canvas` (arcs) + легенда.

#### 7.4 Экраны
Пакет: `presentation/screen/`

**7.4.1 Экран «Расходы»** — `screen/spending/SpendingListScreen.kt`
- Topbar: заголовок «Расходы» + `PeriodFilterChip` (по умолчанию текущий месяц, возможен выбор: месяц / год / кастомный диапазон).
- Счётчик «Транзакций: N».
- `LazyColumn` из `SpendingCard` (группировка по дате — опционально для v1).
- `AddSpendingFab` → открывает `ModalBottomSheet` с формой добавления.
- Долгое нажатие на карточку → меню «Редактировать / Удалить».
- `TotalBar` внизу: «Итого за месяц» + сумма (`#c084fc`).

**7.4.2 Добавление / редактирование расхода** — `screen/spending/AddSpendingSheet.kt` (`ModalBottomSheet`)
- Поля (как в мокапе): **Сумма** (крупный ввод, primary color), **Категория ▾**, **Подкатегория ▾** (в ряд), **Комментарий**, **Дата** (по умолчанию сегодня).
- Кнопка «Сохранить» — фиолетовая (`#7c3aed`).
- Используется и для добавления, и для редактирования (опциональный `spendingId`).

**7.4.3 Экран «Категории»** — `screen/category/CategoryListScreen.kt`
- `LazyColumn` из `CategoryCard`.
- По тапу на карточку — раскрытие списка подкатегорий (`AnimatedVisibility`).
- Бейдж «изм.» → диалог редактирования названия категории / подкатегории.
- FAB для добавления категории (опционально — MVP1 может быть read-only из БД).

**7.4.4 Экран «Сводка»** — `screen/summary/SummaryScreen.kt`
- Topbar: заголовок «Сводка» + `PeriodFilterChip` (год).
- `StatsRow`: «Этот месяц» + «Год».
- Секция «По месяцам» — таблица из `MonthRow`.
- Секция «По категориям с разбивкой» — таблица Категория × Месяц (scrollable horizontally).
- Секция «Диаграмма по категориям» — `PieChartCanvas` + легенда.
- Данные — пока вычисляются на клиенте из `observeSpendings` (агрегации в ViewModel).

**7.4.5 Экран «Профиль»** — `screen/profile/ProfileScreen.kt`
- Аватар с инициалами (линейный градиент `#7c3aed → #4f46e5`).
- Имя, email.
- Строки: **Email**, **Google Таблица** (ссылка «Открыть →» — открывает browser intent), **Ссылка на таблицу** (текст), **Редактировать ссылку** (диалог), **Версия** (`BuildConfig.VERSION_NAME`).
- Кнопка «Выйти» / смены пользователя (если будет в MVP1).

#### 7.5 ViewModels
Пакет: `presentation/viewmodel/` (либо рядом с экранами в `screen/<feature>/`)

Каждая VM использует `StateFlow<UiState>` с sealed-состоянием (`Loading / Data / Error`):
- `SpendingsViewModel` — `observeSpendings(userId)` + фильтр по периоду + расчёт total.
- `AddSpendingViewModel` — форма, валидация, `addSpendingUseCase`.
- `CategoriesViewModel` — `observeCategories` + `observeSubCategories`, редактирование.
- `SummaryViewModel` — агрегации по месяцам и категориям поверх `observeSpendings`.
- `ProfileViewModel` — `observeCurrentUser`, `syncUser(email)`, `clearUser`, обновление ссылки на Google Sheet.

Регистрация в `AppModule` через `viewModelOf(::...)` (Koin `koin-androidx-compose`).

#### 7.6 Текущий пользователь / сессия
- Email текущего пользователя хранить в **DataStore Preferences** (добавить зависимость `androidx.datastore:datastore-preferences`).
- При старте приложения — `ObserveCurrentUserUseCase`, при пустом значении → экран ввода email (или baseline seed для MVP).

### Этап 8: Тестирование и верификация
1. Написать Unit-тесты для UseCases (Spending, Category, User) через fake-репозитории.
2. Написать интеграционные тесты для Room DAO на in-memory базе (`Room.inMemoryDatabaseBuilder`).
3. Проверить сборку релизом: `gradle assembleDebug`.
4. Прогнать на эмуляторе с backend на `http://10.0.2.2:8081`.

---

## 5. Инструкция для пользователя (Как проверять)

### Где смотреть код?
- Весь основной код находится в: `app/src/main/java/spending/tracker/android/`
- Настройки сборки: `app/build.gradle.kts` и `gradle/libs.versions.toml`

### Как запустить?
1. Откройте проект в **Android Studio**.
2. Дождитесь окончания синхронизации Gradle.
3. Выберите эмулятор или подключенное устройство.
4. Нажмите зеленую кнопку **Run** (Shift+F10).

### Как тестировать?
1. **Unit-тесты**: Правой кнопкой на папку `src/test` -> `Run 'Tests in...'`.
2. **Логи**: Вкладка `Logcat` внизу Android Studio.
3. **База данных**: `App Inspection` -> `Database Inspector`.
