package spending.tracker.android.presentation.screen.spendings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import spending.tracker.android.presentation.components.AddSpendingFab
import spending.tracker.android.presentation.components.EmptyState
import spending.tracker.android.presentation.components.PeriodFilter
import spending.tracker.android.presentation.components.PeriodFilterChips
import spending.tracker.android.presentation.components.SpendingCard
import spending.tracker.android.presentation.components.TotalBar
import spending.tracker.android.presentation.theme.CategoryColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val displayDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SpendingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }

    /** Не-null → открыт sheet редактирования для этого id. */
    var editingSpendingId by remember { mutableStateOf<Long?>(null) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Расходы") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        floatingActionButton = {
            AddSpendingFab(onClick = { showAddSheet = true })
        },
        bottomBar = {
            TotalBar(
                periodLabel = "Итого · ${state.period.title.lowercase()}",
                total = state.filteredTotal,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.onRefresh() },
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                PeriodFilterChips(
                    selected = state.period,
                    onSelectedChange = viewModel::onPeriodChanged,
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                // --- Кастомный период ---
                if (state.period == PeriodFilter.Custom) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = { showStartDatePicker = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = state.customDateRange?.startDate?.format(displayDateFormatter)
                                    ?: "Начало",
                            )
                        }
                        Text("—")
                        OutlinedButton(
                            onClick = { showEndDatePicker = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = state.customDateRange?.endDate?.format(displayDateFormatter)
                                    ?: LocalDate.now().format(displayDateFormatter),
                            )
                        }
                    }
                }

                if (state.filteredSpendings.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            title = "Здесь пока пусто",
                            subtitle = "Нажмите «+» чтобы добавить первый расход",
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        items(state.filteredSpendings, key = { it.id }) { spending ->
                            val category = state.categories[spending.categoryId]
                            val colorIndex =
                                (spending.categoryId % CategoryColors.size).toInt()
                            SpendingCard(
                                amount = spending.amount,
                                categoryName = category?.name ?: "Без категории",
                                subCategoryName = spending.subCategoryName,
                                description = spending.description,
                                date = spending.date,
                                accentColor = CategoryColors[colorIndex],
                                onClick = { editingSpendingId = spending.id },
                                onLongClick = { viewModel.onDelete(spending.id) },
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                        item { Spacer(Modifier.height(72.dp)) } // пространство под FAB
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddSpendingSheet(
            onDismiss = { showAddSheet = false },
            spendingId = null,
        )
    }

    editingSpendingId?.let { id ->
        AddSpendingSheet(
            onDismiss = { editingSpendingId = null },
            spendingId = id,
        )
    }

    // --- Date Pickers ---
    if (showStartDatePicker) {
        SpendingDatePickerDialog(
            onDismiss = { showStartDatePicker = false },
            onDateSelected = { date ->
                val currentRange = state.customDateRange
                if (currentRange != null) {
                    viewModel.onCustomDateRangeChanged(date, currentRange.endDate)
                } else {
                    viewModel.onCustomDateRangeChanged(date, date)
                }
                showStartDatePicker = false
            },
        )
    }

    if (showEndDatePicker) {
        SpendingDatePickerDialog(
            onDismiss = { showEndDatePicker = false },
            onDateSelected = { date ->
                val currentRange = state.customDateRange
                if (currentRange != null) {
                    viewModel.onCustomDateRangeChanged(currentRange.startDate, date)
                } else {
                    // При первом выборе даты "до" startDate = сегодня, endDate = выбранная дата
                    viewModel.onCustomDateRangeChanged(LocalDate.now(), date)
                }
                showEndDatePicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpendingDatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(date)
                    }
                },
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}
