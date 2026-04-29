package spending.tracker.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import spending.tracker.android.domain.model.Category
import spending.tracker.android.domain.model.SubCategory
import spending.tracker.android.domain.repository.CategoryRepository

class ObserveCategoriesUseCase(private val repository: CategoryRepository) {
    operator fun invoke(userId: Long): Flow<List<Category>> = repository.observeCategories(userId)
}

class RefreshCategoriesUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(userId: Long): Result<Unit> = repository.refreshCategories(userId)
}

class ObserveSubCategoriesUseCase(private val repository: CategoryRepository) {
    operator fun invoke(categoryId: Long): Flow<List<SubCategory>> =
        repository.observeSubCategories(categoryId)
}

class RefreshSubCategoriesUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(categoryId: Long): Result<Unit> =
        repository.refreshSubCategories(categoryId)
}
