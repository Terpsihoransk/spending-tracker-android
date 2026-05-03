package spending.tracker.android.presentation.screen.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import spending.tracker.android.domain.model.Category
import spending.tracker.android.domain.model.Spending
import spending.tracker.android.domain.usecase.ObserveCategoriesUseCase
import spending.tracker.android.domain.usecase.ObserveCurrentUserUseCase
import spending.tracker.android.domain.usecase.ObserveSpendingsUseCase
import spending.tracker.android.domain.usecase.RefreshCategoriesUseCase
import spending.tracker.android.domain.usecase.RefreshSpendingsUseCase
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** Доступные периоды фильтрации расходов. */
enum class SummaryPeriod(val title: String) {
    Day("День"),
    Week("Неделя"),
    Month("Месяц"),
    Year("Год"),
    Custom("Свой период"),
}

data class DateRange(
    val startDate: LocalDate,
    val endDate: LocalDate,
)

data class SummaryUiState(
    val totalAll: BigDecimal = BigDecimal.ZERO,
    val totalThisMonth: BigDecimal = BigDecimal.ZERO,
    val totalToday: BigDecimal = BigDecimal.ZERO,
    val totalThisWeek: BigDecimal = BigDecimal.ZERO,
    val totalThisYear: BigDecimal = BigDecimal.ZERO,
    /** Суммы по месяцам для выбранного периода. */
    val byMonth: List<MonthTotal> = emptyList(),
    /** Матрица сумм «Категория × Месяц». */
    val categoryByMonth: List<CategoryMonthBreakdown> = emptyList(),
    /** Распределение по категориям за выбранный период. */
    val periodCategories: List<CategoryTotal> = emptyList(),
    /** Выбранный период. */
    val selectedPeriod: SummaryPeriod = SummaryPeriod.Month,
    /** Выбранный диапазон дат (для Custom периода). */
    val customDateRange: DateRange? = null,
    /** Список расходов для отображения в popup. */
    val categorySpendingDetails: List<Spending> = emptyList(),
    /** Категория, для которой показывается popup. */
    val selectedCategoryForDetails: Category? = null,
    /** Флаг видимости popup. */
    val showCategoryDetailsDialog: Boolean = false,
    /** Все расходы (для фильтрации по категории в popup). */
    val allSpendings: List<Spending> = emptyList(),
    /** Флаг активности刷新 (pull-to-refresh). */
    val isRefreshing: Boolean = false,
)

data class MonthTotal(
    val yearMonth: YearMonth,
    val total: BigDecimal,
)

data class CategoryTotal(
    val category: Category,
    val total: BigDecimal,
)

data class CategoryMonthBreakdown(
    val category: Category,
    /** Суммы по месяцам в том же порядке, что и `SummaryUiState.byMonth`. */
    val amounts: List<BigDecimal>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModel(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val observeSpendings: ObserveSpendingsUseCase,
    private val observeCategories: ObserveCategoriesUseCase,
    private val refreshSpendings: RefreshSpendingsUseCase,
    private val refreshCategories: RefreshCategoriesUseCase,
) : ViewModel() {

    private val userEmail: StateFlow<String?> = observeCurrentUser()
        .map { it?.email }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val spendingsFlow = userEmail.flatMapLatest { email ->
        if (email == null) flowOf(emptyList()) else observeSpendings(email)
    }
    private val categoriesFlow = userEmail.flatMapLatest { email ->
        if (email == null) flowOf(emptyList()) else observeCategories(email)
    }

    private val _selectedPeriod = MutableStateFlow(SummaryPeriod.Month)
    private val _customDateRange = MutableStateFlow<DateRange?>(null)
    private val _showCategoryDetailsDialog = MutableStateFlow(false)
    private val _selectedCategoryForDetails = MutableStateFlow<Category?>(null)
    private val isRefreshing = MutableStateFlow(false)

    val state: StateFlow<SummaryUiState> = combine(
        spendingsFlow,
        categoriesFlow,
        _selectedPeriod,
        _customDateRange,
        isRefreshing,
    ) { spendings, categories, period, customRange, refreshing ->
        buildSummary(spendings, categories, period, customRange, refreshing)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SummaryUiState(),
    )

    fun onPeriodChanged(period: SummaryPeriod) {
        _selectedPeriod.value = period
        // При выборе "Свой период" устанавливаем диапазон по умолчанию: сегодня—сегодня
        if (period == SummaryPeriod.Custom && _customDateRange.value == null) {
            val today = LocalDate.now()
            _customDateRange.value = DateRange(today, today)
        }
    }

    fun onCustomDateRangeChanged(startDate: LocalDate, endDate: LocalDate) {
        _customDateRange.value = DateRange(startDate, endDate)
    }

    fun onCategoryClick(category: Category) {
        val currentState = state.value
        val filteredSpendings = filterSpendingsByPeriod(
            currentState.allSpendings,
            currentState.selectedPeriod,
            currentState.customDateRange
        )
        val categorySpendings = filteredSpendings.filter { it.categoryId == category.id }
        _selectedCategoryForDetails.value = category
        _showCategoryDetailsDialog.value = true
        // Update state with category details - we'll use a combined flow for this
    }

    fun onDismissCategoryDetails() {
        _showCategoryDetailsDialog.value = false
        _selectedCategoryForDetails.value = null
    }

    fun refresh() {
        val email = userEmail.value
        if (email == null) {
            isRefreshing.value = false
            return
        }
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                refreshSpendings(email).getOrThrow()
                refreshCategories(email).getOrThrow()
            } catch (e: Exception) {
                // Ошибка обрабатывается внутри getOrThrow() через Result
            } finally {
                isRefreshing.value = false
            }
        }
    }

    private fun filterSpendingsByPeriod(
        spendings: List<Spending>,
        period: SummaryPeriod,
        customRange: DateRange?,
    ): List<Spending> {
        val today = LocalDate.now()
        return when (period) {
            SummaryPeriod.Day -> spendings.filter { it.date == today }
            SummaryPeriod.Week -> {
                val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                spendings.filter { it.date >= startOfWeek && it.date <= endOfWeek }
            }

            SummaryPeriod.Month -> {
                val currentYm = YearMonth.now()
                spendings.filter { YearMonth.from(it.date) == currentYm }
            }

            SummaryPeriod.Year -> {
                val currentYear = today.year
                spendings.filter { it.date.year == currentYear }
            }

            SummaryPeriod.Custom -> {
                customRange?.let { range ->
                    spendings.filter { it.date >= range.startDate && it.date <= range.endDate }
                } ?: emptyList()
            }
        }
    }

    private fun getMonthsForPeriod(
        period: SummaryPeriod,
        customRange: DateRange?,
    ): List<YearMonth> {
        val today = LocalDate.now()
        return when (period) {
            SummaryPeriod.Day -> listOf(YearMonth.from(today))
            SummaryPeriod.Week -> {
                val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                val startYm = YearMonth.from(startOfWeek)
                val endYm = YearMonth.from(endOfWeek)
                generateSequence(startYm) { if (it == endYm) null else it.plusMonths(1) }
                    .takeWhile { it <= endYm }
                    .toList()
            }

            SummaryPeriod.Month -> listOf(YearMonth.now())
            SummaryPeriod.Year -> {
                val currentYear = today.year
                (1..12).map { YearMonth.of(currentYear, it) }
            }

            SummaryPeriod.Custom -> {
                customRange?.let { range ->
                    val startYm = YearMonth.from(range.startDate)
                    val endYm = YearMonth.from(range.endDate)
                    generateSequence(startYm) { if (it == endYm) null else it.plusMonths(1) }
                        .takeWhile { it <= endYm }
                        .toList()
                } ?: listOf(YearMonth.now())
            }
        }
    }

    private fun buildSummary(
        spendings: List<Spending>,
        categories: List<Category>,
        period: SummaryPeriod,
        customRange: DateRange?,
        isRefreshing: Boolean,
    ): SummaryUiState {
        if (spendings.isEmpty()) return SummaryUiState(
            selectedPeriod = period,
            customDateRange = customRange,
            allSpendings = spendings,
            isRefreshing = isRefreshing,
        )

        val today = LocalDate.now()
        val currentYm = YearMonth.now()

        val totalAll = spendings.sumOf { it.amount }
        val totalThisMonth = spendings
            .filter { YearMonth.from(it.date) == currentYm }
            .sumOf { it.amount }
        val totalToday = spendings
            .filter { it.date == today }
            .sumOf { it.amount }

        // Calculate week total
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val totalThisWeek = spendings
            .filter { it.date >= startOfWeek && it.date <= endOfWeek }
            .sumOf { it.amount }

        // Calculate year total
        val totalThisYear = spendings
            .filter { it.date.year == today.year }
            .sumOf { it.amount }

        // Filter spendings for selected period
        val periodSpendings = filterSpendingsByPeriod(spendings, period, customRange)
        val periodMonths = getMonthsForPeriod(period, customRange)

        // Group by month for the period
        val byMonth = periodMonths.map { ym ->
            MonthTotal(
                yearMonth = ym,
                total = periodSpendings.filter { YearMonth.from(it.date) == ym }
                    .sumOf { it.amount },
            )
        }

        // Categories for the period
        val catMap = categories.associateBy { it.id }
        val periodCategories = periodSpendings
            .groupBy { it.categoryId }
            .mapNotNull { (cid, list) ->
                val cat = catMap[cid] ?: return@mapNotNull null
                CategoryTotal(cat, list.sumOf { it.amount })
            }
            .sortedByDescending { it.total }

        // Category by month breakdown for the period
        val categoryByMonth = categories.map { cat ->
            CategoryMonthBreakdown(
                category = cat,
                amounts = periodMonths.map { ym ->
                    periodSpendings.filter { it.categoryId == cat.id && YearMonth.from(it.date) == ym }
                        .sumOf { it.amount }
                },
            )
        }.filter { it.amounts.any { amount -> amount > java.math.BigDecimal.ZERO } }

        return SummaryUiState(
            totalAll = totalAll,
            totalThisMonth = totalThisMonth,
            totalToday = totalToday,
            totalThisWeek = totalThisWeek,
            totalThisYear = totalThisYear,
            byMonth = byMonth,
            categoryByMonth = categoryByMonth,
            periodCategories = periodCategories,
            selectedPeriod = period,
            customDateRange = customRange,
            allSpendings = spendings,
            isRefreshing = isRefreshing,
        )
    }
}
