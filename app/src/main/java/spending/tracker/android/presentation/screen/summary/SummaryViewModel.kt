package spending.tracker.android.presentation.screen.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import spending.tracker.android.domain.model.Category
import spending.tracker.android.domain.model.Spending
import spending.tracker.android.domain.usecase.ObserveCategoriesUseCase
import spending.tracker.android.domain.usecase.ObserveCurrentUserUseCase
import spending.tracker.android.domain.usecase.ObserveSpendingsUseCase
import java.time.LocalDate
import java.time.YearMonth

data class SummaryUiState(
    val totalAll: Double = 0.0,
    val totalThisMonth: Double = 0.0,
    val totalToday: Double = 0.0,
    val averagePerDay: Double = 0.0,
    /** Суммы по месяцам (последние 6 месяцев). */
    val byMonth: List<MonthTotal> = emptyList(),
    /** Матрица сумм «Категория × Месяц». */
    val categoryByMonth: List<CategoryMonthBreakdown> = emptyList(),
    /** Распределение по категориям за текущий месяц. */
    val currentMonthCategories: List<CategoryTotal> = emptyList(),
)

data class MonthTotal(
    val yearMonth: YearMonth,
    val total: Double,
)

data class CategoryTotal(
    val category: Category,
    val total: Double,
)

data class CategoryMonthBreakdown(
    val category: Category,
    /** Суммы по месяцам в том же порядке, что и `SummaryUiState.byMonth`. */
    val amounts: List<Double>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModel(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val observeSpendings: ObserveSpendingsUseCase,
    private val observeCategories: ObserveCategoriesUseCase,
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

    val state: StateFlow<SummaryUiState> = combine(
        spendingsFlow,
        categoriesFlow,
    ) { spendings, categories ->
        buildSummary(spendings, categories)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SummaryUiState(),
    )

    private fun buildSummary(spendings: List<Spending>, categories: List<Category>): SummaryUiState {
        if (spendings.isEmpty()) return SummaryUiState()
        val today = LocalDate.now()
        val currentYm = YearMonth.now()
        val lastMonths = (0..5).map { currentYm.minusMonths(it.toLong()) }.reversed()

        val totalAll = spendings.sumOf { it.amount }
        val totalThisMonth = spendings
            .filter { YearMonth.from(it.date) == currentYm }
            .sumOf { it.amount }
        val totalToday = spendings
            .filter { it.date == today }
            .sumOf { it.amount }

        val dayOfMonth = today.dayOfMonth
        val avgPerDay = if (dayOfMonth > 0) totalThisMonth / dayOfMonth else 0.0

        val byMonth = lastMonths.map { ym ->
            MonthTotal(
                yearMonth = ym,
                total = spendings.filter { YearMonth.from(it.date) == ym }.sumOf { it.amount },
            )
        }

        val catMap = categories.associateBy { it.id }
        val currentMonthCategories = spendings
            .filter { YearMonth.from(it.date) == currentYm }
            .groupBy { it.categoryId }
            .mapNotNull { (cid, list) ->
                val cat = catMap[cid] ?: return@mapNotNull null
                CategoryTotal(cat, list.sumOf { it.amount })
            }
            .sortedByDescending { it.total }

        val categoryByMonth = categories.map { cat ->
            CategoryMonthBreakdown(
                category = cat,
                amounts = lastMonths.map { ym ->
                    spendings.filter { it.categoryId == cat.id && YearMonth.from(it.date) == ym }
                        .sumOf { it.amount }
                },
            )
        }.filter { it.amounts.any { amount -> amount > 0.0 } }

        return SummaryUiState(
            totalAll = totalAll,
            totalThisMonth = totalThisMonth,
            totalToday = totalToday,
            averagePerDay = avgPerDay,
            byMonth = byMonth,
            categoryByMonth = categoryByMonth,
            currentMonthCategories = currentMonthCategories,
        )
    }
}
