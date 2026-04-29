package spending.tracker.android.presentation.screen.spendings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import spending.tracker.android.domain.model.Category
import spending.tracker.android.domain.model.SubCategory

private const val MAX_DESCRIPTION_LENGTH = 40

/**
 * Modal-bottom-sheet для добавления или редактирования расхода.
 *
 * Дата расхода управляется сервером — клиент не выбирает дату,
 * а получает её из ответа бэкенда.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSpendingSheet(
    onDismiss: () -> Unit,
    spendingId: Long? = null,
    viewModel: AddSpendingViewModel = koinViewModel(key = "add_spending_$spendingId") {
        parametersOf(spendingId)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // При каждом открытии sheet в режиме редактирования — загружаем данные заново.
    LaunchedEffect(spendingId) {
        spendingId?.let { viewModel.onEditOpened(it) }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.reset()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = if (state.isEditMode) "Редактирование расхода" else "Новый расход",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))

            if (state.isInitialLoading) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                OutlinedTextField(
                    value = state.amountInput,
                    onValueChange = viewModel::onAmountChange,
                    label = { Text("Сумма, ₽") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))

                CategoryDropdown(
                    categories = state.categories,
                    selectedId = state.selectedCategoryId,
                    onSelect = viewModel::onCategoryChange,
                    onAddNew = viewModel::addCategory,
                )
                Spacer(Modifier.height(12.dp))

                SubCategoryDropdown(
                    subCategories = state.subCategories,
                    selectedId = state.selectedSubCategoryId,
                    onSelect = viewModel::onSubCategoryChange,
                    onAddNew = viewModel::addSubCategory,
                    enabled = state.selectedCategoryId != null,
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.description,
                    onValueChange = { if (it.length <= MAX_DESCRIPTION_LENGTH) viewModel.onDescriptionChange(it) },
                    label = { Text("Комментарий (необязательно)") },
                    singleLine = true,
                    supportingText = { Text("${state.description.length}/$MAX_DESCRIPTION_LENGTH") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = {
                    scope.launch { sheetState.hide() }
                    viewModel.reset()
                    onDismiss()
                }) {
                    Text("Отмена")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = state.canSubmit,
                    onClick = {
                        viewModel.submit {
                            scope.launch { sheetState.hide() }
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(
                        when {
                            state.isSubmitting -> "Сохранение…"
                            state.isEditMode -> "Сохранить"
                            else -> "Добавить"
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onAddNew: (String, () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    val selectedName = categories.firstOrNull { it.id == selectedId }?.name ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Категория") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        DropdownMenuContent(
            expanded = expanded,
            onDismiss = { expanded = false },
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    },
                )
            }
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Добавить категорию", color = MaterialTheme.colorScheme.primary)
                    }
                },
                onClick = {
                    expanded = false
                    showAddDialog = true
                },
            )
        }
    }

    if (showAddDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                newCategoryName = ""
            },
            title = { Text("Новая категория") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            onAddNew(newCategoryName) {
                                showAddDialog = false
                                newCategoryName = ""
                            }
                        }
                    },
                    enabled = newCategoryName.isNotBlank(),
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    newCategoryName = ""
                }) {
                    Text("Отмена")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubCategoryDropdown(
    subCategories: List<SubCategory>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onAddNew: (String, () -> Unit) -> Unit,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newSubCategoryName by remember { mutableStateOf("") }
    val selectedName = subCategories.firstOrNull { it.id == selectedId }?.name ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Подкатегория (необязательно)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        DropdownMenuContent(
            expanded = expanded,
            onDismiss = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("— без подкатегории —") },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            subCategories.forEach { sub ->
                DropdownMenuItem(
                    text = { Text(sub.name) },
                    onClick = {
                        onSelect(sub.id)
                        expanded = false
                    },
                )
            }
            if (enabled) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Добавить подкатегорию", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    onClick = {
                        expanded = false
                        showAddDialog = true
                    },
                )
            }
        }
    }

    if (showAddDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                newSubCategoryName = ""
            },
            title = { Text("Новая подкатегория") },
            text = {
                OutlinedTextField(
                    value = newSubCategoryName,
                    onValueChange = { newSubCategoryName = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSubCategoryName.isNotBlank()) {
                            onAddNew(newSubCategoryName) {
                                showAddDialog = false
                                newSubCategoryName = ""
                            }
                        }
                    },
                    enabled = newSubCategoryName.isNotBlank(),
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    newSubCategoryName = ""
                }) {
                    Text("Отмена")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdownMenuBoxScope.DropdownMenuContent(
    expanded: Boolean,
    onDismiss: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        content = content,
    )
}
