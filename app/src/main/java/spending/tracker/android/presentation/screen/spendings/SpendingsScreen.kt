package spending.tracker.android.presentation.screen.spendings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import spending.tracker.android.presentation.components.PeriodFilterChips
import spending.tracker.android.presentation.components.SpendingCard
import spending.tracker.android.presentation.components.TotalBar
import spending.tracker.android.presentation.theme.CategoryColors

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
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            PeriodFilterChips(
                selected = state.period,
                onSelectedChange = viewModel::onPeriodChanged,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            if (state.filteredSpendings.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        title = "Здесь пока пусто",
                        subtitle = "Нажмите «+» чтобы добавить первый расход",
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    items(state.filteredSpendings, key = { it.id }) { spending ->
                        val category = state.categories[spending.categoryId]
                        val colorIndex =
                            (spending.categoryId % CategoryColors.size).toInt()
                        SpendingCard(
                            amount = spending.amount,
                            categoryName = category?.name ?: "Без категории",
                            subCategoryName = null, // имя подкатегории не грузим в список — для MVP1 опционально
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
}
