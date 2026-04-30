package spending.tracker.android.presentation.screen.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.flowOf
import org.koin.androidx.compose.koinViewModel
import spending.tracker.android.domain.model.SubCategory
import spending.tracker.android.presentation.components.AddSpendingFab
import spending.tracker.android.presentation.components.EmptyState
import spending.tracker.android.presentation.theme.CategoryColors

/** Что сейчас редактируется / создаётся в диалоге. */
private sealed interface CategoryDialog {
    data object AddCategory : CategoryDialog
    data class EditCategory(val id: Long, val initialName: String) : CategoryDialog
    data class DeleteCategory(val id: Long, val name: String) : CategoryDialog

    data class AddSubCategory(val categoryId: Long) : CategoryDialog
    data class EditSubCategory(val id: Long, val categoryId: Long, val initialName: String) : CategoryDialog
    data class DeleteSubCategory(val id: Long, val name: String) : CategoryDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    modifier: Modifier = Modifier,
    viewModel: CategoriesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<CategoryDialog?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Показывать ошибки из ViewModel
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Категории") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        floatingActionButton = {
            AddSpendingFab(onClick = { dialog = CategoryDialog.AddCategory })
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            if (state.items.isEmpty() && !state.isLoading) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        title = "Нет категорий",
                        subtitle = "Нажмите «+» чтобы добавить первую категорию",
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.category.id }) { item ->
                        val expanded = item.category.id in state.expandedIds
                        val subsState = if (expanded) {
                            viewModel.subCategoriesFlow(item.category.id)
                                .collectAsStateWithLifecycle(initialValue = emptyList())
                        } else {
                            flowOf(emptyList<SubCategory>())
                                .collectAsStateWithLifecycle(initialValue = emptyList())
                        }
                        val colorIndex = (item.category.id % CategoryColors.size).toInt()

                        EditableCategoryCard(
                            name = item.category.name,
                            accentColor = CategoryColors[colorIndex],
                            subCategories = subsState.value,
                            expanded = expanded,
                            onToggle = { viewModel.toggle(item.category.id) },
                            onEdit = {
                                dialog = CategoryDialog.EditCategory(item.category.id, item.category.name)
                            },
                            onDelete = {
                                dialog = CategoryDialog.DeleteCategory(item.category.id, item.category.name)
                            },
                            onAddSub = {
                                dialog = CategoryDialog.AddSubCategory(item.category.id)
                            },
                            onEditSub = { sub ->
                                dialog = CategoryDialog.EditSubCategory(sub.id, sub.categoryId, sub.name)
                            },
                            onDeleteSub = { sub ->
                                dialog = CategoryDialog.DeleteSubCategory(sub.id, sub.name)
                            },
                        )
                    }
                }
            }
        }
    }

    // ---- Диалоги ----
    when (val d = dialog) {
        null -> Unit
        CategoryDialog.AddCategory -> NameDialog(
            title = "Новая категория",
            initial = "",
            onConfirm = { name ->
                viewModel.onAddCategory(name)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        is CategoryDialog.EditCategory -> NameDialog(
            title = "Переименовать категорию",
            initial = d.initialName,
            onConfirm = { name ->
                viewModel.onUpdateCategory(d.id, name)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        is CategoryDialog.DeleteCategory -> ConfirmDialog(
            title = "Удалить категорию?",
            message = "Категория «${d.name}» будет удалена. Если она используется в расходах, удаление будет отклонено бэкендом.",
            onConfirm = {
                viewModel.onDeleteCategory(d.id)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        is CategoryDialog.AddSubCategory -> NameDialog(
            title = "Новая подкатегория",
            initial = "",
            onConfirm = { name ->
                viewModel.onAddSubCategory(d.categoryId, name)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        is CategoryDialog.EditSubCategory -> NameDialog(
            title = "Переименовать подкатегорию",
            initial = d.initialName,
            onConfirm = { name ->
                viewModel.onUpdateSubCategory(d.id, d.categoryId, name)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        is CategoryDialog.DeleteSubCategory -> ConfirmDialog(
            title = "Удалить подкатегорию?",
            message = "Подкатегория «${d.name}» будет удалена. Если она используется в расходах, удаление будет отклонено бэкендом.",
            onConfirm = {
                viewModel.onDeleteSubCategory(d.id)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
    }
}

/** Универсальный диалог с полем «имя». */
@Composable
private fun NameDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text("Название") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotBlank(),
                onClick = { onConfirm(value.trim()) },
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

/** Простой confirm-диалог. */
@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Удалить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

/** Карточка категории с иконками редактирования/удаления и списком подкатегорий. */
@Composable
private fun EditableCategoryCard(
    name: String,
    accentColor: Color,
    subCategories: List<SubCategory>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddSub: () -> Unit,
    onEditSub: (SubCategory) -> Unit,
    onDeleteSub: (SubCategory) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Индикатор цвета
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(accentColor, CircleShape),
                )
                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onToggle),
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Редактировать",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    if (subCategories.isEmpty()) {
                        Text(
                            text = "Нет подкатегорий",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 36.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                        )
                    } else {
                        subCategories.forEach { sub ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 36.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "• ${sub.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { onEditSub(sub) }) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "Редактировать",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { onDeleteSub(sub) }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Удалить",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                    TextButton(
                        onClick = onAddSub,
                        modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Добавить подкатегорию")
                    }
                }
            }
        }
    }
}
