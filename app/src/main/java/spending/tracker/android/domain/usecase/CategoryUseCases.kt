package spending.tracker.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import spending.tracker.android.domain.model.Category
import spending.tracker.android.domain.model.SubCategory
import spending.tracker.android.domain.repository.CategoryRepository

// ---------- Categories ----------

class ObserveCategoriesUseCase(private val repository: CategoryRepository) {
    operator fun invoke(userEmail: String): Flow<List<Category>> =
        repository.observeCategories(userEmail)
}

class RefreshCategoriesUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(userEmail: String): Result<Unit> =
        repository.refreshCategories(userEmail)
}

class AddCategoryUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(userEmail: String, name: String): Result<Category> =
        repository.addCategory(userEmail, name)
}

class UpdateCategoryUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(userEmail: String, id: Long, name: String): Result<Category> =
        repository.updateCategory(userEmail, id, name)
}

class DeleteCategoryUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(userEmail: String, id: Long): Result<Unit> =
        repository.deleteCategory(userEmail, id)
}

// ---------- SubCategories ----------

class ObserveSubCategoriesUseCase(private val repository: CategoryRepository) {
    operator fun invoke(categoryId: Long): Flow<List<SubCategory>> =
        repository.observeSubCategories(categoryId)
}

class RefreshSubCategoriesUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(userEmail: String, categoryId: Long): Result<Unit> =
        repository.refreshSubCategories(userEmail, categoryId)
}

class AddSubCategoryUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(
        userEmail: String,
        categoryId: Long,
        name: String,
    ): Result<SubCategory> = repository.addSubCategory(userEmail, categoryId, name)
}

class UpdateSubCategoryUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(
        userEmail: String,
        id: Long,
        categoryId: Long,
        name: String,
    ): Result<SubCategory> = repository.updateSubCategory(userEmail, id, categoryId, name)
}

class DeleteSubCategoryUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(userEmail: String, id: Long): Result<Unit> =
        repository.deleteSubCategory(userEmail, id)
}
