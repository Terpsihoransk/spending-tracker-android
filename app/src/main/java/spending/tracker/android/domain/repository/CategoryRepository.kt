package spending.tracker.android.domain.repository

import kotlinx.coroutines.flow.Flow
import spending.tracker.android.domain.model.Category
import spending.tracker.android.domain.model.SubCategory

/**
 * Offline-first репозиторий категорий/подкатегорий.
 *
 * UI всегда подписывается на [observeCategories] / [observeSubCategories].
 * [refreshCategories] / [refreshSubCategories] обновляют локальный кэш из сети.
 * CRUD-операции синхронно вызывают бэк и обновляют локальный кэш.
 */
interface CategoryRepository {
    // ---- Categories ----
    fun observeCategories(userEmail: String): Flow<List<Category>>
    suspend fun refreshCategories(userEmail: String): Result<Unit>
    suspend fun addCategory(userEmail: String, name: String): Result<Category>
    suspend fun updateCategory(userEmail: String, id: Long, name: String): Result<Category>
    suspend fun deleteCategory(userEmail: String, id: Long): Result<Unit>

    // ---- SubCategories ----
    fun observeSubCategories(categoryId: Long): Flow<List<SubCategory>>
    suspend fun refreshSubCategories(userEmail: String, categoryId: Long): Result<Unit>
    suspend fun addSubCategory(userEmail: String, categoryId: Long, name: String): Result<SubCategory>
    suspend fun updateSubCategory(userEmail: String, id: Long, categoryId: Long, name: String): Result<SubCategory>
    suspend fun deleteSubCategory(userEmail: String, id: Long): Result<Unit>
}
