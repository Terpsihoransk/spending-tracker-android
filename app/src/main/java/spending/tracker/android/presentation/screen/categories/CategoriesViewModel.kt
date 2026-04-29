package spending.tracker.android.presentation.screen.categories

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
import spending.tracker.android.domain.model.SubCategory
import spending.tracker.android.domain.usecase.AddCategoryUseCase
import spending.tracker.android.domain.usecase.AddSubCategoryUseCase
import spending.tracker.android.domain.usecase.DeleteCategoryUseCase
import spending.tracker.android.domain.usecase.DeleteSubCategoryUseCase
import spending.tracker.android.domain.usecase.ObserveCategoriesUseCase
import spending.tracker.android.domain.usecase.ObserveCurrentUserUseCase
import spending.tracker.android.domain.usecase.ObserveSubCategoriesUseCase
import spending.tracker.android.domain.usecase.RefreshCategoriesUseCase
import spending.tracker.android.domain.usecase.RefreshSubCategoriesUseCase
import spending.tracker.android.domain.usecase.UpdateCategoryUseCase
import spending.tracker.android.domain.usecase.UpdateSubCategoryUseCase

/** Пара «категория → её подкатегории» для UI. */
data class CategoryWithSubs(
    val category: Category,
    val subCategories: List<SubCategory>,
)

data class CategoriesUiState(
    val items: List<CategoryWithSubs> = emptyList(),
    val expandedIds: Set<Long> = emptySet(),
    val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModel(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val observeCategories: ObserveCategoriesUseCase,
    private val refreshCategories: RefreshCategoriesUseCase,
    private val observeSubCategories: ObserveSubCategoriesUseCase,
    private val refreshSubCategories: RefreshSubCategoriesUseCase,
    private val addCategory: AddCategoryUseCase,
    private val updateCategory: UpdateCategoryUseCase,
    private val deleteCategory: DeleteCategoryUseCase,
    private val addSubCategory: AddSubCategoryUseCase,
    private val updateSubCategory: UpdateSubCategoryUseCase,
    private val deleteSubCategory: DeleteSubCategoryUseCase,
) : ViewModel() {

    private val expanded = MutableStateFlow<Set<Long>>(emptySet())
    private val error = MutableStateFlow<String?>(null)

    private val userEmail: StateFlow<String?> = observeCurrentUser()
        .map { it?.email }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val categoriesFlow = userEmail.flatMapLatest { email ->
        if (email == null) flowOf(emptyList()) else observeCategories(email)
    }

    val state: StateFlow<CategoriesUiState> = combine(
        categoriesFlow,
        expanded,
        error,
    ) { cats, exp, err ->
        CategoriesUiState(
            items = cats.map { CategoryWithSubs(it, emptyList()) },
            expandedIds = exp,
            errorMessage = err,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CategoriesUiState(),
    )

    init {
        viewModelScope.launch {
            userEmail.collect { email ->
                if (email != null) {
                    refreshCategories(email).onFailure { error.value = it.message }
                }
            }
        }
    }

    /** Подписка на подкатегории раскрытой карточки (вызывается из UI по требованию). */
    fun subCategoriesFlow(categoryId: Long) = observeSubCategories(categoryId)

    fun toggle(categoryId: Long) {
        expanded.value = if (expanded.value.contains(categoryId)) {
            expanded.value - categoryId
        } else {
            val email = userEmail.value
            if (email != null) {
                viewModelScope.launch {
                    refreshSubCategories(email, categoryId).onFailure { error.value = it.message }
                }
            }
            expanded.value + categoryId
        }
    }

    // ---- CRUD Category ----

    fun onAddCategory(name: String) {
        val email = userEmail.value ?: return
        viewModelScope.launch {
            addCategory(email, name).onFailure { error.value = it.message }
        }
    }

    fun onUpdateCategory(id: Long, name: String) {
        val email = userEmail.value ?: return
        viewModelScope.launch {
            updateCategory(email, id, name).onFailure { error.value = it.message }
        }
    }

    fun onDeleteCategory(id: Long) {
        val email = userEmail.value ?: return
        viewModelScope.launch {
            deleteCategory(email, id).onFailure { error.value = it.message }
        }
    }

    // ---- CRUD SubCategory ----

    fun onAddSubCategory(categoryId: Long, name: String) {
        val email = userEmail.value ?: return
        viewModelScope.launch {
            addSubCategory(email, categoryId, name).onFailure { error.value = it.message }
        }
    }

    fun onUpdateSubCategory(id: Long, categoryId: Long, name: String) {
        val email = userEmail.value ?: return
        viewModelScope.launch {
            updateSubCategory(email, id, categoryId, name).onFailure { error.value = it.message }
        }
    }

    fun onDeleteSubCategory(id: Long) {
        val email = userEmail.value ?: return
        viewModelScope.launch {
            deleteSubCategory(email, id).onFailure { error.value = it.message }
        }
    }

    fun clearError() {
        error.value = null
    }
}
