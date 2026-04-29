package spending.tracker.android.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import spending.tracker.android.data.local.dao.CategoryDao
import spending.tracker.android.data.remote.api.CategoryApi
import spending.tracker.android.domain.model.Category
import spending.tracker.android.domain.model.SubCategory
import spending.tracker.android.domain.repository.CategoryRepository
import spending.tracker.android.util.toDomain
import spending.tracker.android.util.toEntity

class CategoryRepositoryImpl(
    private val api: CategoryApi,
    private val dao: CategoryDao
) : CategoryRepository {

    override fun observeCategories(userId: Long): Flow<List<Category>> =
        dao.observeCategories(userId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun refreshCategories(userId: Long): Result<Unit> = runCatching {
        val remote = api.getCategories(userId)
        dao.replaceCategoriesForUser(userId, remote.map { it.toEntity() })
    }.onFailure { Log.w(TAG, "refreshCategories(userId=$userId) failed", it) }

    override fun observeSubCategories(categoryId: Long): Flow<List<SubCategory>> =
        dao.observeSubCategories(categoryId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun refreshSubCategories(categoryId: Long): Result<Unit> = runCatching {
        val remote = api.getSubCategories(categoryId)
        dao.replaceSubCategoriesForCategory(categoryId, remote.map { it.toEntity() })
    }.onFailure { Log.w(TAG, "refreshSubCategories(categoryId=$categoryId) failed", it) }

    private companion object {
        const val TAG = "CategoryRepository"
    }
}
