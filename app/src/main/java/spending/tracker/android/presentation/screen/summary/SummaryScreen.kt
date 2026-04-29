package spending.tracker.android.presentation.screen.summary

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import spending.tracker.android.presentation.components.EmptyState
import spending.tracker.android.presentation.components.PieChartCanvas
import spending.tracker.android.presentation.components.PieChartLegend
import spending.tracker.android.presentation.components.PieSlice
import spending.tracker.android.presentation.components.StatCard
import spending.tracker.android.presentation.components.StatRow
import spending.tracker.android.presentation.theme.CategoryColors
import spending.tracker.android.util.formatMoney
import spending.tracker.android.util.formatMonthShort

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    modifier: Modifier = Modifier,
    viewModel: SummaryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
            StatRow(
                left = {
                    StatCard(
                        label = "За месяц",
                        value = formatMoney(state.totalThisMonth),
                    )
                },
                right = {
                    StatCard(
                        label = "Сегодня",
                        value = formatMoney(state.totalToday),
                    )
                },
            )
            StatRow(
                left = {
                    StatCard(
                        label = "В среднем в день",
                        value = formatMoney(state.averagePerDay),
                    )
                },
                right = {
                    StatCard(
                        label = "Всего",
                        value = formatMoney(state.totalAll),
                    )
                },
            )

            // --- По месяцам ---
            SectionCard(title = "По месяцам (последние 6)") {
                state.byMonth.forEach { mt ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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

            // --- Категория × месяц ---
            if (state.categoryByMonth.isNotEmpty()) {
                SectionCard(title = "Категория × Месяц") {
                    // Шапка месяцев
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(
                            "",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        state.byMonth.forEach { mt ->
                            Text(
                                text = formatMonthShort(mt.yearMonth),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    state.categoryByMonth.forEach { row ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = row.category.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            row.amounts.forEach { amount ->
                                Text(
                                    text = if (amount == 0.0) "–" else formatMoney(amount),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // --- Диаграмма текущего месяца ---
            if (state.currentMonthCategories.isNotEmpty()) {
                SectionCard(title = "Распределение за этот месяц") {
                    val slices = state.currentMonthCategories.mapIndexed { index, entry ->
                        PieSlice(
                            label = entry.category.name,
                            value = entry.total,
                            color = CategoryColors[index % CategoryColors.size],
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PieChartCanvas(
                            slices = slices,
                            modifier = Modifier.size(140.dp),
                        )
                        Spacer(Modifier.size(16.dp))
                        PieChartLegend(slices = slices, modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
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
