package spending.tracker.android.presentation.screen.spendings

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
import spending.tracker.android.domain.usecase.DeleteSpendingUseCase
import spending.tracker.android.domain.usecase.ObserveCategoriesUseCase
import spending.tracker.android.domain.usecase.ObserveCurrentUserUseCase
import spending.tracker.android.domain.usecase.ObserveSpendingsUseCase
import spending.tracker.android.domain.usecase.RefreshCategoriesUseCase
import spending.tracker.android.domain.usecase.RefreshSpendingsUseCase
import spending.tracker.android.presentation.components.PeriodFilter
import java.time.LocalDate

/** Диапазон дат для custom периода. */
data class DateRange(
    val startDate: LocalDate,
    val endDate: LocalDate,
)

/** Состояние экрана «Расходы». */
data class SpendingsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val spendings: List<Spending> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val period: PeriodFilter = PeriodFilter.Today,
    val customDateRange: DateRange? = null,
    val errorMessage: String? = null,
) {
    /** Отфильтрованный по [period] список. */
    val filteredSpendings: List<Spending>
        get() {
            val today = LocalDate.now()
            return spendings.filter { spending ->
                when (period) {
                    PeriodFilter.Today -> spending.date == today
                    PeriodFilter.Week -> spending.date >= today.minusDays(6)
                    PeriodFilter.Month -> spending.date.year == today.year &&
                            spending.date.month == today.month

                    PeriodFilter.Year -> spending.date.year == today.year
                    PeriodFilter.Custom -> {
                        customDateRange?.let { range ->
                            spending.date >= range.startDate && spending.date <= range.endDate
                        } ?: true
                    }
                }
            }
        }

    /** Сумма за отфильтрованный период. */
    val filteredTotal: Double
        get() = filteredSpendings.sumOf { it.amount }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SpendingsViewModel(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val observeSpendings: ObserveSpendingsUseCase,
    private val refreshSpendings: RefreshSpendingsUseCase,
    private val observeCategories: ObserveCategoriesUseCase,
    private val refreshCategories: RefreshCategoriesUseCase,
    private val deleteSpending: DeleteSpendingUseCase,
) : ViewModel() {

    private val period = MutableStateFlow(PeriodFilter.Today)
    private val customDateRange = MutableStateFlow<DateRange?>(null)
    private val error = MutableStateFlow<String?>(null)
    private val isRefreshing = MutableStateFlow(false)

    /** Текущий email пользователя (идентификатор на бэке). */
    val currentUserEmail: StateFlow<String?> = observeCurrentUser()
        .map { it?.email }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val spendingsFlow = currentUserEmail.flatMapLatest { email ->
        if (email == null) flowOf(emptyList()) else observeSpendings(email)
    }

    private val categoriesFlow = currentUserEmail.flatMapLatest { email ->
        if (email == null) flowOf(emptyList()) else observeCategories(email)
    }

    val state: StateFlow<SpendingsUiState> = combine(
        spendingsFlow,
        categoriesFlow,
        period,
        customDateRange,
        error,
        isRefreshing,
    ) { array: Array<*> ->
        @Suppress("UNCHECKED_CAST")
        val spendings = array[0] as List<Spending>
        val categories = array[1] as List<Category>
        val p = array[2] as PeriodFilter
        val range = array[3] as DateRange?
        val err = array[4] as String?
        val refreshing = array[5] as Boolean
        SpendingsUiState(
            isLoading = false,
            isRefreshing = refreshing,
            spendings = spendings.sortedByDescending { it.date },
            categories = categories.associateBy { it.id },
            period = p,
            customDateRange = range,
            errorMessage = err,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SpendingsUiState(isLoading = true),
    )

    init {
        viewModelScope.launch {
            currentUserEmail.collect { email ->
                if (email != null) {
                    refreshSpendings(email).onFailure { error.value = it.message }
                    refreshCategories(email).onFailure { error.value = it.message }
                }
            }
        }
    }

    fun onPeriodChanged(newPeriod: PeriodFilter) {
        period.value = newPeriod
        // При выборе "Свой период" устанавливаем диапазон по умолчанию: сегодня—сегодня
        if (newPeriod == PeriodFilter.Custom && customDateRange.value == null) {
            val today = LocalDate.now()
            customDateRange.value = DateRange(today, today)
        }
    }

    fun onCustomDateRangeChanged(startDate: LocalDate, endDate: LocalDate) {
        customDateRange.value = DateRange(startDate, endDate)
    }

    fun onRefresh() {
        val email = currentUserEmail.value
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
                error.value = e.message
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun onDelete(id: Long) {
        viewModelScope.launch {
            val email = currentUserEmail.value ?: return@launch
            deleteSpending(email, id).onFailure { error.value = it.message }
        }
    }
}
