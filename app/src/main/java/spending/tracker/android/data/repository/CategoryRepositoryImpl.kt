package spending.tracker.android.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import spending.tracker.android.data.local.dao.CategoryDao
import spending.tracker.android.data.remote.api.CategoryApi
import spending.tracker.android.data.remote.api.SubCategoryApi
import spending.tracker.android.data.remote.dto.CategoryRequest
import spending.tracker.android.data.remote.dto.SubCategoryRequest
import spending.tracker.android.domain.model.Category
import spending.tracker.android.domain.model.SubCategory
import spending.tracker.android.domain.repository.CategoryRepository
import spending.tracker.android.util.toDomain
import spending.tracker.android.util.toEntity

class CategoryRepositoryImpl(
    private val categoryApi: CategoryApi,
    private val subCategoryApi: SubCategoryApi,
    private val dao: CategoryDao,
) : CategoryRepository {

    // ----- Categories -----

    override fun observeCategories(userEmail: String): Flow<List<Category>> =
        dao.observeCategories(userEmail).map { entities -> entities.map { it.toDomain() } }

    override suspend fun refreshCategories(userEmail: String): Result<Unit> = runCatching {
        val remote = categoryApi.getCategories(userEmail)
        dao.syncCategoriesForUser(userEmail, remote.map { it.toEntity() })
    }.onFailure { Log.w(TAG, "refreshCategories(userEmail=$userEmail) failed", it) }

    override suspend fun addCategory(userEmail: String, name: String): Result<Category> = runCatching {
        val remote = categoryApi.createCategory(userEmail, CategoryRequest(name = name))
        dao.upsertCategory(remote.toEntity())
        remote.toDomain()
    }.onFailure { Log.w(TAG, "addCategory(name=$name) failed", it) }

    override suspend fun updateCategory(userEmail: String, id: Long, name: String): Result<Category> = runCatching {
        val remote = categoryApi.updateCategory(userEmail, id, CategoryRequest(name = name))
        dao.upsertCategory(remote.toEntity())
        remote.toDomain()
    }.onFailure { Log.w(TAG, "updateCategory(id=$id) failed", it) }

    override suspend fun deleteCategory(userEmail: String, id: Long): Result<Unit> = runCatching {
        categoryApi.deleteCategory(userEmail, id)
        dao.deleteCategory(id)
    }.onFailure { Log.w(TAG, "deleteCategory(id=$id) failed", it) }

    // ----- SubCategories -----

    override fun observeSubCategories(categoryId: Long): Flow<List<SubCategory>> =
        dao.observeSubCategories(categoryId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun refreshSubCategories(userEmail: String, categoryId: Long): Result<Unit> = runCatching {
        val remote = subCategoryApi.getSubCategories(userEmail, categoryId)
        dao.syncSubCategoriesForCategory(categoryId, remote.map { it.toEntity() })
    }.onFailure { Log.w(TAG, "refreshSubCategories(categoryId=$categoryId) failed", it) }

    override suspend fun addSubCategory(
        userEmail: String,
        categoryId: Long,
        name: String,
    ): Result<SubCategory> = runCatching {
        val remote = subCategoryApi.createSubCategory(
            userEmail,
            SubCategoryRequest(name = name, categoryId = categoryId),
        )
        dao.upsertSubCategory(remote.toEntity())
        remote.toDomain()
    }.onFailure { Log.w(TAG, "addSubCategory(name=$name, categoryId=$categoryId) failed", it) }

    override suspend fun updateSubCategory(
        userEmail: String,
        id: Long,
        categoryId: Long,
        name: String,
    ): Result<SubCategory> = runCatching {
        val remote = subCategoryApi.updateSubCategory(
            userEmail,
            id,
            SubCategoryRequest(name = name, categoryId = categoryId),
        )
        dao.upsertSubCategory(remote.toEntity())
        remote.toDomain()
    }.onFailure { Log.w(TAG, "updateSubCategory(id=$id) failed", it) }

    override suspend fun deleteSubCategory(userEmail: String, id: Long): Result<Unit> = runCatching {
        subCategoryApi.deleteSubCategory(userEmail, id)
        dao.deleteSubCategory(id)
    }.onFailure { Log.w(TAG, "deleteSubCategory(id=$id) failed", it) }

    private companion object {
        const val TAG = "CategoryRepository"
    }
}
