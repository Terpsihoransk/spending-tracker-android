package spending.tracker.android.presentation.screen.spendings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import spending.tracker.android.domain.model.Category
import spending.tracker.android.domain.model.SubCategory
import spending.tracker.android.domain.usecase.AddSpendingUseCase
import spending.tracker.android.domain.usecase.AddSubCategoryUseCase
import spending.tracker.android.domain.usecase.AddCategoryUseCase
import spending.tracker.android.domain.usecase.DeleteSpendingUseCase
import spending.tracker.android.domain.usecase.GetSpendingByIdUseCase
import spending.tracker.android.domain.usecase.ObserveCategoriesUseCase
import spending.tracker.android.domain.usecase.ObserveCurrentUserUseCase
import spending.tracker.android.domain.usecase.ObserveSubCategoriesUseCase
import spending.tracker.android.domain.usecase.RefreshSubCategoriesUseCase
import spending.tracker.android.domain.usecase.UpdateSpendingUseCase
import spending.tracker.android.util.formatDateDmYyyy
import spending.tracker.android.util.parseDateDmYyyy
import java.math.BigDecimal
import java.time.LocalDate

/** Состояние Bottom-Sheet'а «Добавить/Редактировать расход». */
data class AddSpendingFormState(
    val amountInput: String = "",
    val selectedCategoryId: Long? = null,
    val selectedSubCategoryId: Long? = null,
    val description: String = "",
    val categories: List<Category> = emptyList(),
    val subCategories: List<SubCategory> = emptyList(),
    val isEditMode: Boolean = false,
    val isInitialLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    /** Дата в формате dd.MM.yyyy для отображения в TextField. */
    val dateInput: String = "",
    /** Дата редактируемого расхода (для передачи в API). */
    val editingDate: LocalDate? = null,
) {
    /** Можно ли сабмитить форму. */
    val canSubmit: Boolean
        get() {
            if (isSubmitting || isInitialLoading || selectedCategoryId == null) return false
            val parsed = amountInput.toBigDecimalOrNull() ?: return false
            return parsed > BigDecimal.ZERO
        }
}

/** Внутреннее состояние редактирования формы (без сетевых списков). */
private data class FormEdits(
    val amountInput: String = "",
    val selectedCategoryId: Long? = null,
    val selectedSubCategoryId: Long? = null,
    val description: String = "",
    val isEditMode: Boolean = false,
    val isInitialLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    /** Дата в формате dd.MM.yyyy для отображения в TextField. */
    val dateInput: String = "",
    /** Дата редактируемого расхода (для передачи в API). */
    val editingDate: LocalDate? = null,
)

/**
 * ViewModel для экрана «Добавить/Редактировать расход».
 *
 * Если [spendingId] не null — режим редактирования: форма заполняется
 * данными существующего расхода, а submit вызывает [updateSpending].
 * Иначе — режим создания: submit вызывает [addSpending].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddSpendingViewModel(
    private val spendingId: Long? = null,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val observeCategories: ObserveCategoriesUseCase,
    private val observeSubCategories: ObserveSubCategoriesUseCase,
    private val refreshSubCategories: RefreshSubCategoriesUseCase,
    private val addSpending: AddSpendingUseCase,
    private val updateSpending: UpdateSpendingUseCase,
    private val getSpendingById: GetSpendingByIdUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val addSubCategoryUseCase: AddSubCategoryUseCase,
    private val deleteSpendingUseCase: DeleteSpendingUseCase,
) : ViewModel() {

    private val edits = MutableStateFlow(
        FormEdits(isEditMode = spendingId != null, isInitialLoading = spendingId != null)
    )

    private val userEmail: StateFlow<String?> = observeCurrentUser()
        .map { it?.email }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val categoriesFlow = userEmail.flatMapLatest { email ->
        if (email == null) flowOf(emptyList()) else observeCategories(email)
    }

    private val subCategoriesFlow = edits
        .map { it.selectedCategoryId }
        .flatMapLatest { cid ->
            if (cid == null) flowOf(emptyList()) else observeSubCategories(cid)
        }

    val state: StateFlow<AddSpendingFormState> = combine(
        edits,
        categoriesFlow,
        subCategoriesFlow,
    ) { e, cats, subs ->
        AddSpendingFormState(
            amountInput = e.amountInput,
            selectedCategoryId = e.selectedCategoryId,
            selectedSubCategoryId = e.selectedSubCategoryId,
            description = e.description,
            categories = cats,
            subCategories = subs,
            isEditMode = e.isEditMode,
            isInitialLoading = e.isInitialLoading,
            isSubmitting = e.isSubmitting,
            errorMessage = e.errorMessage,
            dateInput = e.dateInput,
            editingDate = e.editingDate,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AddSpendingFormState(
            isEditMode = spendingId != null,
            isInitialLoading = spendingId != null,
        ),
    )

    init {
        if (spendingId != null) {
            preloadSpending(spendingId)
        }
    }

    /**
     * Вызывается при каждом открытии sheet в режиме редактирования.
     * Сбрасывает состояние и загружает данные заново.
     */
    fun onEditOpened(id: Long) {
        edits.value = FormEdits(isEditMode = true, isInitialLoading = true)
        preloadSpending(id)
    }

    /** Загрузить существующий расход с бэка по ID. */
    private fun preloadSpending(id: Long) {
        viewModelScope.launch {
            val email = userEmail.filterNotNull().first()
            getSpendingById(email, id).fold(
                onSuccess = { spending ->
                    edits.update {
                        it.copy(
                            amountInput = spending.amount.toPlainString(),
                            selectedCategoryId = spending.categoryId,
                            selectedSubCategoryId = spending.subCategoryId,
                            description = spending.description.orEmpty(),
                            isInitialLoading = false,
                            dateInput = formatDateDmYyyy(spending.date),
                            editingDate = spending.date,
                        )
                    }
                },
                onFailure = {
                    edits.update {
                        it.copy(
                            isInitialLoading = false,
                            errorMessage = "Не удалось загрузить расход"
                        )
                    }
                },
            )
        }
    }

    fun onAmountChange(value: String) {
        val sanitized = value.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
        edits.update { it.copy(amountInput = sanitized) }
    }

    fun onCategoryChange(id: Long) {
        edits.update { it.copy(selectedCategoryId = id, selectedSubCategoryId = null) }
        // Подгружаем подкатегории с сервера при смене категории
        val email = userEmail.value ?: return
        viewModelScope.launch {
            refreshSubCategories(email, id)
        }
    }

    fun onSubCategoryChange(id: Long?) {
        edits.update { it.copy(selectedSubCategoryId = id) }
    }

    fun onDescriptionChange(value: String) {
        edits.update { it.copy(description = value) }
    }

    /**
     * Обработчик изменения даты.
     * Парсит дату из формата dd.MM.yyyy и сохраняет в editingDate.
     */
    fun onDateChange(value: String) {
        val parsed = try {
            parseDateDmYyyy(value)
        } catch (e: Exception) {
            null
        }
        edits.update { it.copy(dateInput = value, editingDate = parsed) }
    }

    /** Добавить новую категорию и выбрать её. */
    fun addCategory(name: String, onSuccess: () -> Unit) {
        val email = userEmail.value ?: return
        viewModelScope.launch {
            addCategoryUseCase(email, name).fold(
                onSuccess = { category ->
                    edits.update { it.copy(selectedCategoryId = category.id, selectedSubCategoryId = null) }
                    onSuccess()
                },
                onFailure = { err ->
                    edits.update { it.copy(errorMessage = err.message ?: "Не удалось создать категорию") }
                },
            )
        }
    }

    /** Добавить новую подкатегорию и выбрать её. */
    fun addSubCategory(name: String, onSuccess: () -> Unit) {
        val email = userEmail.value ?: return
        val categoryId = edits.value.selectedCategoryId ?: return
        viewModelScope.launch {
            addSubCategoryUseCase(email, categoryId, name).fold(
                onSuccess = { subCategory ->
                    edits.update { it.copy(selectedSubCategoryId = subCategory.id) }
                    onSuccess()
                },
                onFailure = { err ->
                    edits.update { it.copy(errorMessage = err.message ?: "Не удалось создать подкатегорию") }
                },
            )
        }
    }

    fun clearError() {
        edits.update { it.copy(errorMessage = null) }
    }

    /** Сброс формы (после успешного сабмита / закрытия sheet). */
    fun reset() {
        edits.value = FormEdits(isEditMode = spendingId != null)
    }

    /** Удалить расход и закрыть sheet. */
    fun deleteSpending(onSuccess: () -> Unit) {
        val id = spendingId ?: return
        val email = userEmail.value ?: return
        viewModelScope.launch {
            deleteSpendingUseCase(email, id).fold(
                onSuccess = {
                    reset()
                    onSuccess()
                },
                onFailure = { err ->
                    edits.update {
                        it.copy(errorMessage = err.message ?: "Не удалось удалить")
                    }
                },
            )
        }
    }

    /** Попытка сабмита. [onSuccess] вызывается при успехе — для закрытия sheet. */
    fun submit(onSuccess: () -> Unit) {
        val s = state.value
        val email = userEmail.value
        val amount = s.amountInput.toBigDecimalOrNull()
        val cid = s.selectedCategoryId
        if (email == null || amount == null || amount <= BigDecimal.ZERO || cid == null) {
            edits.update { it.copy(errorMessage = "Заполните сумму и категорию") }
            return
        }
        edits.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val result = if (spendingId != null) {
                // Если dateInput изменён — парсим его, иначе используем editingDate или текущую дату
                val date = edits.value.dateInput.takeIf { it.isNotBlank() }?.let {
                    try {
                        parseDateDmYyyy(it)
                    } catch (e: Exception) {
                        null
                    }
                } ?: edits.value.editingDate ?: LocalDate.now()
                updateSpending(
                    userEmail = email,
                    id = spendingId,
                    amount = amount,
                    categoryId = cid,
                    subCategoryId = s.selectedSubCategoryId,
                    description = s.description.ifBlank { null },
                    date = date,
                )
            } else {
                addSpending(
                    userEmail = email,
                    amount = amount,
                    categoryId = cid,
                    subCategoryId = s.selectedSubCategoryId,
                    description = s.description.ifBlank { null },
                )
            }
            result.fold(
                onSuccess = {
                    reset()
                    onSuccess()
                },
                onFailure = { err ->
                    edits.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = err.message ?: "Не удалось сохранить"
                        )
                    }
                },
            )
        }
    }
}

// --- helpers ---

private fun <T> MutableStateFlow<T>.update(block: (T) -> T) {
    value = block(value)
}