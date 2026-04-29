package spending.tracker.android.presentation.screen.summary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import spending.tracker.android.presentation.components.EmptyState
import spending.tracker.android.presentation.components.PieSlice
import spending.tracker.android.presentation.components.StatCard
import spending.tracker.android.presentation.components.StatRow
import spending.tracker.android.presentation.theme.CategoryColors
import spending.tracker.android.util.formatMoney
import spending.tracker.android.util.formatMonthShort
import spending.tracker.android.util.formatRelativeDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    modifier: Modifier = Modifier,
    viewModel: SummaryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showCategoryDetailsDialog by remember { mutableStateOf(false) }
    var selectedCategoryForDetails by remember { mutableStateOf<spending.tracker.android.domain.model.Category?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Сводка") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.totalAll == 0.0) {
            Box(
                Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    title = "Нет данных",
                    subtitle = "Добавьте несколько расходов, чтобы увидеть сводку",
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // --- Период фильтр ---
            SummaryPeriodChips(
                selected = state.selectedPeriod,
                onSelectedChange = viewModel::onPeriodChanged,
            )

            // --- Кастомный период ---
            if (state.selectedPeriod == SummaryPeriod.Custom) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            text = state.customDateRange?.startDate?.toString() ?: "Начало",
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
                            text = state.customDateRange?.endDate?.toString() ?: "Конец",
                        )
                    }
                }
            }

            // --- Статистика в одну строку: сегодня, за месяц, всего ---
            StatRow(
                left = {
                    StatCard(
                        label = "Сегодня",
                        value = formatMoney(state.totalToday),
                    )
                },
                middle = {
                    StatCard(
                        label = "За месяц",
                        value = formatMoney(state.totalThisMonth),
                    )
                },
                right = {
                    StatCard(
                        label = "Всего",
                        value = formatMoney(state.totalAll),
                    )
                },
                showThreeColumns = true,
            )

            // --- Расходы по месяцам ---
            if (state.byMonth.isNotEmpty()) {
                SectionCard(title = "Расходы по месяцам") {
                    state.byMonth.forEach { mt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = formatMonthShort(mt.yearMonth),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = formatMoney(mt.total),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // --- Расходы по категориям ---
            if (state.periodCategories.isNotEmpty()) {
                SectionCard(title = "Расходы по категориям") {
                    Column(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Категория",
                                modifier = Modifier.width(120.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Сумма",
                                modifier = Modifier.width(100.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        state.periodCategories.forEach { catTotal ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = catTotal.category.name,
                                    modifier = Modifier.width(120.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = formatMoney(catTotal.total),
                                    modifier = Modifier.width(100.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // --- Диаграмма за период ---
            if (state.periodCategories.isNotEmpty()) {
                SectionCard(title = "Распределение за период") {
                    val slices = state.periodCategories.mapIndexed { index, entry ->
                        PieSlice(
                            label = entry.category.name,
                            value = entry.total,
                            color = CategoryColors[index % CategoryColors.size],
                            category = entry.category,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ClickablePieChart(
                            slices = slices,
                            modifier = Modifier.size(140.dp),
                            onSliceClick = { category ->
                                selectedCategoryForDetails = category
                                showCategoryDetailsDialog = true
                            },
                        )
                        Spacer(Modifier.size(16.dp))
                        PieChartLegend(
                            slices = slices,
                            modifier = Modifier.weight(1f),
                            onSliceClick = { category ->
                                selectedCategoryForDetails = category
                                showCategoryDetailsDialog = true
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // --- Dialog для деталей категории ---
        if (showCategoryDetailsDialog && selectedCategoryForDetails != null) {
            CategoryDetailsDialog(
                category = selectedCategoryForDetails!!,
                spendings = state.allSpendings.filter {
                    it.categoryId == selectedCategoryForDetails!!.id &&
                            isInPeriod(it.date, state.selectedPeriod, state.customDateRange)
                },
                onDismiss = {
                    showCategoryDetailsDialog = false
                    selectedCategoryForDetails = null
                },
            )
        }

        // --- Date Pickers ---
        if (showStartDatePicker) {
            DatePickerDialogWrapper(
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
            DatePickerDialogWrapper(
                onDismiss = { showEndDatePicker = false },
                onDateSelected = { date ->
                    val currentRange = state.customDateRange
                    if (currentRange != null) {
                        viewModel.onCustomDateRangeChanged(currentRange.startDate, date)
                    } else {
                        viewModel.onCustomDateRangeChanged(date, date)
                    }
                    showEndDatePicker = false
                },
            )
        }
    }
}

private fun isInPeriod(
    date: LocalDate,
    period: SummaryPeriod,
    customRange: DateRange?,
): Boolean {
    return when (period) {
        SummaryPeriod.Day -> date == LocalDate.now()
        SummaryPeriod.Week -> {
            val today = LocalDate.now()
            val startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            val endOfWeek = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))
            date in startOfWeek..endOfWeek
        }
        SummaryPeriod.Month -> java.time.YearMonth.from(date) == java.time.YearMonth.now()
        SummaryPeriod.Year -> date.year == LocalDate.now().year
        SummaryPeriod.Custom -> {
            customRange?.let { date >= it.startDate && date <= it.endDate } ?: false
        }
    }
}

@Composable
private fun SummaryPeriodChips(
    selected: SummaryPeriod,
    onSelectedChange: (SummaryPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(SummaryPeriod.entries.toList()) { period ->
            FilterChip(
                selected = period == selected,
                onClick = { onSelectedChange(period) },
                label = {
                    Text(
                        text = period.title,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = period == selected,
                    borderColor = MaterialTheme.colorScheme.outline,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

@Composable
private fun ClickablePieChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 40f,
    onSliceClick: (spending.tracker.android.domain.model.Category) -> Unit,
) {
    val total = slices.sumOf { it.value }.takeIf { it > 0.0 } ?: 1.0
    
    Canvas(
        modifier = modifier.clickable(enabled = false) {},
    ) {
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(
            x = (size.width - diameter) / 2f,
            y = (size.height - diameter) / 2f,
        )
        val arcSize = Size(diameter, diameter)

        var startAngle = -90f
        slices.forEach { slice ->
            val sweep = (slice.value / total * 360.0).toFloat()
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth),
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun PieChartLegend(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    onSliceClick: (spending.tracker.android.domain.model.Category) -> Unit,
) {
    val total = slices.sumOf { it.value }.takeIf { it > 0.0 } ?: 1.0
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        slices.forEach { slice ->
            Row(
                modifier = Modifier.clickable { slice.category?.let { onSliceClick(it) } },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(slice.color, CircleShape),
                )
                Spacer(Modifier.padding(horizontal = 4.dp))
                val pct = (slice.value / total * 100).toInt()
                Text(
                    text = "${slice.label} · $pct%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun CategoryDetailsDialog(
    category: spending.tracker.android.domain.model.Category,
    spendings: List<spending.tracker.android.domain.model.Spending>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleLarge,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                    )
                }
            }
        },
        text = {
            val totalBySubCategory = spendings
                .groupBy { it.subCategoryName ?: "Без подкатегории" }
                .mapValues { it.value.sumOf { s -> s.amount } }
                .toList()
                .sortedByDescending { it.second }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        text = "Всего: ${formatMoney(spendings.sumOf { it.amount })}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }

                items(totalBySubCategory) { (subCategoryName, amount) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = subCategoryName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = formatMoney(amount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (spendings.size > 1) {
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = "Всего расходов: ${spendings.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogWrapper(
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
                Text("ОК")
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

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
