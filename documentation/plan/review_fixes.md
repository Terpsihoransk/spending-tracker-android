# План правок по результатам code review (MVP1)

> Документ зафиксирован после ревью текущей реализации «как senior Kotlin разработчик».
> Содержит конкретные замечания, приоритеты и предлагаемые решения.

---

## 🔴 P0 — Критичные (чинить в первую очередь)

### ~~P0-1. Race / утечка данных при logout (смена пользователя)~~
- **Файл:** [`ProfileViewModel.logout()`](../app/src/main/java/spending/tracker/android/presentation/screen/profile/ProfileViewModel.kt:41), [`UserRepositoryImpl.clearUser()`](../app/src/main/java/spending/tracker/android/data/repository/UserRepositoryImpl.kt:29)
- **Проблема:** при `clearUser()` чистится только таблица `users`. Таблицы `spendings`, `categories`, `subcategories` остаются с данными предыдущего аккаунта. При логине новым email данные будут видны до первого `refresh`.
- **Фикс:** расширить `clearUser()` (или ввести `LogoutUseCase`), чтобы вычищать **все таблицы** (или сразу делать `db.clearAllTables()`). Также чистить DataStore атомарно.
- **Решение:** При logout необходимо очищать все таблицы одним способом - appDatabase.clearAllTables()

### P0-2. `amount: Double` для денег
- **Файл:** [`Mappers.kt:38`](../app/src/main/java/spending/tracker/android/util/Mappers.kt:38), [`Spending.kt`](../app/src/main/java/spending/tracker/android/domain/model/Spending.kt:7), [`SpendingEntity.kt`](../app/src/main/java/spending/tracker/android/data/local/entity/SpendingEntity.kt:18)
- **Проблема:**
  1. `amount.toDoubleOrNull() ?: 0.0` — при невалидной строке с бэка пользователь увидит `0` вместо ошибки.
  2. `Double` для денежных сумм = ошибки округления (`0.1 + 0.2 != 0.3`).
- **Фикс:** перейти на `BigDecimal` в domain + DTO остаётся `String`, в Entity — `String` (`amount.toPlainString()`). При ошибке парсинга — бросать исключение, чтобы `runCatching` в репозитории поймал.

### P0-3. Моргание UI при refresh (DELETE + INSERT)
- **Файл:** [`SpendingDao.replaceAllForUser`](../app/src/main/java/spending/tracker/android/data/local/dao/SpendingDao.kt:33), [`CategoryDao.replaceCategoriesForUser`](../app/src/main/java/spending/tracker/android/data/local/dao/CategoryDao.kt:31)
- **Проблема:** внутри `@Transaction` Flow может эмитить промежуточное пустое состояние — UI моргает пустым списком на каждом refresh. `@Transaction` на **suspend-функциях** с несколькими отдельными запросами НЕ гарантирует отсутствие промежуточных эмиссий в observe-Flow.
- **Фикс:** заменить на «умный sync»:
  ```kotlin
  @Query("DELETE FROM spendings WHERE userEmail = :userEmail AND id NOT IN (:keepIds)")
  suspend fun deleteMissingForUser(userEmail: String, keepIds: List<Long>)

  @Transaction
  suspend fun syncForUser(userEmail: String, remote: List<SpendingEntity>) {
      deleteMissingForUser(userEmail, remote.map { it.id })
      upsertSpendings(remote)
  }
  ```

### P0-4. `BASE_URL` захардкожен на `10.0.2.2:8081`
- **Файл:** [`HttpClientFactory.kt:20`](../app/src/main/java/spending/tracker/android/data/remote/HttpClientFactory.kt:20)
- **Проблема:** работает только на эмуляторе. Любой release-build или реальное устройство упадут.
- **Фикс:** вынести в `BuildConfig` через `buildConfigField` в [`app/build.gradle.kts`](../../app/build.gradle.kts) с разными значениями для `debug` / `release`. Включить `buildFeatures.buildConfig = true`.

### P0-5. Подкатегории не подгружаются для `AddSpendingSheet`
- **Файл:** [`CategoriesViewModel.toggle`](../app/src/main/java/spending/tracker/android/presentation/screen/categories/CategoriesViewModel.kt:96), [`AddSpendingViewModel`](../app/src/main/java/spending/tracker/android/presentation/screen/spendings/AddSpendingViewModel.kt:1)
- **Проблема:** `refreshSubCategories` дёргается **только** при раскрытии карточки на экране «Категории». В `AddSpendingSheet` при выборе категории показывается пустой список — пока юзер не откроет «Категории» и не раскроет каждую.
- **Фикс:** в `AddSpendingViewModel.onCategoryChange` автоматически вызывать `refreshSubCategories(email, categoryId)`. Либо при `refreshCategories` делать bulk-refresh всех субкатегорий.

---

## ИИ ревью. Перед правкой необходимо проверить актуальность

* При изменении периода фильтрации (onPeriodChanged) не сбрасывается диапазон дат для Custom периода, что может привести к отображению данных по старому диапазону при
последующем выборе Custom.

```app/src/main/java/spending/tracker/android/presentation/screen/summary/SummaryViewModel.kt [116-118]

fun onPeriodChanged(period: SummaryPeriod) {
_selectedPeriod.value = period
+    if (period != SummaryPeriod.Custom) {
+        _customDateRange.value = null
+    }
     }
```
Критическая ошибка в логике приложения. При переключении между периодами пользователь может видеть данные, отфильтрованные по старому кастомному диапазону, 
что приведет к неверной информации. Сброс _customDateRange при выходе из режима Custom необходим для корректной работы фильтрации.

* Передача текущей даты как параметра
Метод isInPeriod использует LocalDate.now()внутри, что нарушает чистоту функции и может привести к неконсистентному поведению при вызовах в течение одного дня.
Следует передавать текущую дату как параметр.

```app/src/main/java/spending/tracker/android/presentation/screen/summary/SummaryScreen.kt [354-366]

private fun isInPeriod(
date: LocalDate,
period: SummaryPeriod,
customRange: DateRange?,
+    today: LocalDate = LocalDate.now(),
     ): Boolean {
     return when (period) {
-        SummaryPeriod.Day -> date == LocalDate.now()
+        SummaryPeriod.Day -> date == today
         SummaryPeriod.Week -> {
-            val today = LocalDate.now()
             val startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
             val endOfWeek = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))
             date in startOfWeek..endOfWeek
         }
```

Предложение выявляет важную проблему с чистотой функции и потенциальной неконсистентностью поведения при использовании LocalDate.now() внутри функции. 
Это может привести к ошибкам в логике фильтрации, особенно при тестировании или при вызовах вблизи полуночи.


* Проверка выбора даты перед подтверждением
В DatePickerDialogWrapper отсутствует обработка случая, когда selectedDateMillis равен null при нажатии кнопки подтверждения. 
Это может привести к игнорированию действия без обратной связи.

```app/src/main/java/spending/tracker/android/presentation/screen/summary/SummaryScreen.kt [577-591]

confirmButton = {
TextButton(
onClick = {
datePickerState.selectedDateMillis?.let { millis ->
val date = Instant.ofEpochMilli(millis)
.atZone(ZoneId.systemDefault())
.toLocalDate()
onDateSelected(date)
+            } ?: run {
+                // Можно добавить визуальную обратную связь, если дата не выбрана
             }
         },
+        enabled = datePickerState.selectedDateMillis != null,
  ) {
  Text("ОК")
  }
  },
```
Сuggestion правильно указывает на отсутствие обработки случая, когда дата не выбрана. Хотя использование enabled = datePickerState.selectedDateMillis != null уже предотвращает основную проблему, явная обработка или хотя бы визуальная обратная связь улучшит UX. Текущая реализация может запутать пользователя.

---

## 🟡 P1 — Важные архитектурные замечания

### P1-6. Денормализация `categoryName` / `subCategoryName` в `SpendingEntity`
- **Файл:** [`SpendingEntity.kt:20`](../app/src/main/java/spending/tracker/android/data/local/entity/SpendingEntity.kt:20)
- **Проблема:** при переименовании категории все расходы продолжают показывать старое имя до `refreshSpendings`.
- **Фикс на выбор:**
  - (a) Убрать из entity имена, использовать `@Relation` / join в DAO.
  - (b) После `updateCategory` автоматически триггерить `refreshSpendings`.

### P1-7. UseCases — тривиальные обёртки, 90% бойлерплейта
- **Файл:** [`SpendingUseCases.kt`](../../app/src/main/java/spending/tracker/android/domain/usecase/SpendingUseCases.kt), [`CategoryUseCases.kt`](../../app/src/main/java/spending/tracker/android/domain/usecase/CategoryUseCases.kt), [`UserUseCases.kt`](../../app/src/main/java/spending/tracker/android/domain/usecase/UserUseCases.kt), [`AppModule.kt`](../app/src/main/java/spending/tracker/android/di/AppModule.kt:79)
- **Проблема:** 15+ классов, которые ничего не делают кроме делегирования в репозиторий. Это cargo-cult Clean Architecture.
- **Фикс на выбор:**
  - (a) **Предпочтительно:** удалить тривиальные UseCases, инжектить репозитории напрямую в ViewModel (подход Google NowInAndroid). Оставить UseCases только там, где реальная бизнес-логика (валидация, композиция нескольких репо).
  - (b) Либо сохранить UseCases, но добавить в них осмысленную логику.

### P1-8. Тяжёлые вычисления в `getter`-ах UI-state
- **Файл:** [`SpendingsUiState`](../app/src/main/java/spending/tracker/android/presentation/screen/spendings/SpendingsViewModel.kt:27)
- **Проблема:** `filteredSpendings` и `filteredTotal` — вычисляемые `val get()`. Compose вызовет их при каждой рекомпозиции.
- **Фикс:** перенести вычисления в `combine { ... }` внутри ViewModel, класть уже готовый `List<Spending>` + `Double` в state.

### P1-9. Тихое проглатывание ошибок везде
- **Файл:** все репозитории ([`SpendingRepositoryImpl`](../../app/src/main/java/spending/tracker/android/data/repository/SpendingRepositoryImpl.kt), [`CategoryRepositoryImpl`](../../app/src/main/java/spending/tracker/android/data/repository/CategoryRepositoryImpl.kt), [`UserRepositoryImpl`](../../app/src/main/java/spending/tracker/android/data/repository/UserRepositoryImpl.kt))
- **Проблема:** `runCatching { ... }.onFailure { Log.w(...) }` — ошибки уходят только в logcat. UI об ошибках refresh никогда не узнает (показываем только из явных `onFailure` во ViewModel).
- **Фикс:**
  - Ввести `sealed interface UiState<T> { Loading; Content(T); Error(Throwable) }` на ключевых экранах.
  - Показывать `Snackbar` / toast при ошибках.
  - Отдельная обработка `IOException` (нет сети) vs `ClientRequestException` (ошибка бэка).

### P1-10. `PUT /spending/{id}` без `date` — возможная потеря даты при редактировании
- **Файл:** [`SpendingRepositoryImpl.updateSpending`](../app/src/main/java/spending/tracker/android/data/repository/SpendingRepositoryImpl.kt:46), [`SpendingRequest`](../app/src/main/java/spending/tracker/android/data/remote/dto/Dtos.kt:55)
- **Проблема:** `POST` не передаёт `date` — сервер ставит `now`. В `PUT` тоже не передаётся. Если сервер обрабатывает `PUT` симметрично — **дата расхода затрётся на `now` при каждом редактировании**.
- **Фикс:** уточнить по `API_SPECIFICATION.md`, как сервер обрабатывает `PUT` без `date`. Если перезаписывает — добавить поле `date` в `SpendingRequest` (опциональное) и передавать в `updateSpending`.

---

## 🟢 P2 — Мелкие улучшения / стиль

### P2-11. Свой `MutableStateFlow.update`
- **Файл:** [`AddSpendingViewModel.kt:230`](../app/src/main/java/spending/tracker/android/presentation/screen/spendings/AddSpendingViewModel.kt:230)
- **Фикс:** удалить, использовать стандартный `kotlinx.coroutines.flow.update`.

### P2-12. Неиспользуемое `categories: Map<Long, Category>` в `SpendingsUiState`
- **Файл:** [`SpendingsViewModel.kt:30`](../app/src/main/java/spending/tracker/android/presentation/screen/spendings/SpendingsViewModel.kt:30)
- **Фикс:** убрать — `spending.categoryName` и так в entity денормализован (или оставить, если заберём из entity — см. P1-6).

### P2-13. Flow-collect внутри `LazyColumn items { ... }`
- **Файл:** [`CategoriesScreen.kt:116-122`](../app/src/main/java/spending/tracker/android/presentation/screen/categories/CategoriesScreen.kt:116)
- **Проблема:** на каждый item создаётся отдельный `collectAsStateWithLifecycle`, плюс на закрытых создаётся `flowOf(emptyList<SubCategory>())` заново на каждой рекомпозиции.
- **Фикс:** хранить `subCategories` прямо в `CategoryWithSubs` в state, собирать map `categoryId → List<SubCategory>` в `combine` внутри ViewModel (через `flatMapLatest` от `expanded`).

### P2-14. `HttpClient` не закрывается
- Для Android-процесса ОК (процесс живёт до OOM-killer), но добавить комментарий / TODO.

### P2-15. `UserApi.getUserByEmail` качает весь список
- **Файл:** [`Apis.kt:117`](../app/src/main/java/spending/tracker/android/data/remote/api/Apis.kt:117)
- **Фикс:** поставить TODO, попросить бэк добавить `GET /user?email=...` или `GET /user/{email}`.

### P2-16. `fallbackToDestructiveMigration(dropAllTables = true)` в релизе
- **Файл:** [`AppModule.kt:57`](../app/src/main/java/spending/tracker/android/di/AppModule.kt:57)
- **Фикс:** перед первым продакшн-релизом написать нормальные миграции + добавить `@AutoMigration`.

### P2-17. `exportSchema = false`
- **Файл:** [`AppDatabase.kt:21`](../app/src/main/java/spending/tracker/android/data/local/database/AppDatabase.kt:21)
- **Фикс:** перед релизом поставить `true`, указать `room.schemaLocation` в KSP-аргументах, коммитить схемы в git.

### P2-18. Inconsistency `subcategoryId` vs `subCategoryId`
- **Файлы:** DTO ([`Dtos.kt`](../../app/src/main/java/spending/tracker/android/data/remote/dto/Dtos.kt)) vs domain/entity.
- **Фикс:** привести к одному стилю. Либо `@SerialName("subcategoryId")` на поле `subCategoryId` во всех DTO.

### P2-19. Нет интеграционных тестов репозиториев
- **Фикс:** добавить связку `RepoImpl` + fake `Api` + реальный in-memory Room DAO. Именно там живут баги маппинга DTO ↔ Entity ↔ Domain.

### P2-20. Нет instrumented (androidTest) тестов для UI
- **Фикс:** минимум 1–2 smoke-теста: навигация + добавление расхода.

### P2-21. Прочие мелочи
- `Double.toPlainString()` в [`AddSpendingViewModel.kt:234`](../app/src/main/java/spending/tracker/android/presentation/screen/spendings/AddSpendingViewModel.kt:234) — свой, ограниченный. Для денег уйдёт вместе с переходом на `BigDecimal` (P0-2).
- Разнести [`Mappers.kt`](../../app/src/main/java/spending/tracker/android/util/Mappers.kt) по файлам (`UserMappers.kt`, `SpendingMappers.kt`, ...).
- `errorMessage: String?` — не i18n-friendly. Либо `@StringRes Int`, либо sealed `ErrorType`.
- `applicationId == namespace` — допустимо, но обычно добавляют `.debug` суффикс для debug-билда.
- Удалить заброшенный [`app/src/main/java/spending/tracker/android/BakingViewModel.kt`](../app/src/main/java/spending/tracker/android/BakingViewModel.kt) (если он ещё существует — в checklist помечен как «отсутствовал»).

---

## ✅ Что уже хорошо — сохранить

- Clean Architecture соблюдена, domain без Android-зависимостей.
- Flow-first API репозиториев (observe + refresh) — прямо школа Google.
- `StateFlow` + `WhileSubscribed(5_000)` — канонично.
- Koin `viewModelOf` + параметризованный `AddSpendingViewModel` через `parametersOf` — красиво.
- `LocalDateSerializer` вынесен в util — правильно.
- `@Index` на FK-колонках.
- DataStore вместо SharedPreferences.
- Единый стиль error-handling (`runCatching` + `Log.w`).
- Актуальный стек: Kotlin 2.3.21, Compose BOM 2026.04, Material 3, AGP 9.2.0.
- Тесты с Turbine + coroutines-test + Robolectric.

---

## 📊 Итоговая оценка (для истории)

| Категория | Оценка |
|-----------|--------|
| Архитектура | 7/10 |
| Надёжность | 6/10 |
| Идиоматичность Kotlin | 8/10 |
| Compose-практики | 7/10 |
| Тестируемость | 6/10 |
| Production-ready | 5/10 |

**Вердикт:** Как MVP — зачёт. Как prod-кандидат — нужна ещё одна итерация (P0 + P1).

---

