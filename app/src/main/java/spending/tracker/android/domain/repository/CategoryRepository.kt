package spending.tracker.android.domain.repository

import kotlinx.coroutines.flow.Flow
import spending.tracker.android.domain.model.Category
import spending.tracker.android.domain.model.SubCategory

/**
 * Offline-first репозиторий категорий/подкатегорий.
 *
 * UI всегда подписывается на [observeCategories] / [observeSubCategories].
 * [refreshCategories] / [refreshSubCategories] обновляют локальный кэш из сети.
 */
interface CategoryRepository {
    fun observeCategories(userId: Long): Flow<List<Category>>
    suspend fun refreshCategories(userId: Long): Result<Unit>

    fun observeSubCategories(categoryId: Long): Flow<List<SubCategory>>
    suspend fun refreshSubCategories(categoryId: Long): Result<Unit>
}
