package spending.tracker.android.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import spending.tracker.android.data.local.dao.SpendingDao
import spending.tracker.android.data.remote.api.SpendingApi
import spending.tracker.android.data.remote.dto.SpendingRequest
import spending.tracker.android.domain.model.Spending
import spending.tracker.android.domain.repository.SpendingRepository
import spending.tracker.android.util.toDomain
import spending.tracker.android.util.toEntity

class SpendingRepositoryImpl(
    private val api: SpendingApi,
    private val dao: SpendingDao,
) : SpendingRepository {

    override fun observeSpendings(userEmail: String): Flow<List<Spending>> =
        dao.observeSpendings(userEmail).map { entities -> entities.map { it.toDomain() } }

    override suspend fun refreshSpendings(userEmail: String): Result<Unit> = runCatching {
        val remote = api.getSpendings(userEmail)
        dao.replaceAllForUser(userEmail, remote.map { it.toEntity(synced = true) })
    }.onFailure { Log.w(TAG, "refreshSpendings(userEmail=$userEmail) failed", it) }

    override suspend fun getSpendingById(userEmail: String, id: Long): Result<Spending> = runCatching {
        val remote = api.getSpending(userEmail, id)
        dao.upsertSpending(remote.toEntity(synced = true))
        remote.toDomain()
    }.onFailure { Log.w(TAG, "getSpendingById(id=$id) failed", it) }

    override suspend fun addSpending(
        userEmail: String,
        amount: Double,
        categoryId: Long,
        subCategoryId: Long?,
        description: String?,
    ): Result<Spending> = runCatching {
        val request = SpendingRequest(
            amount = amount,
            categoryId = categoryId,
            subcategoryId = subCategoryId,
            description = description,
        )
        val remote = api.createSpending(userEmail, request)
        val entity = remote.toEntity(synced = true)
        dao.upsertSpending(entity)
        remote.toDomain()
    }.onFailure { Log.w(TAG, "addSpending failed", it) }

    override suspend fun updateSpending(
        userEmail: String,
        id: Long,
        amount: Double,
        categoryId: Long,
        subCategoryId: Long?,
        description: String?,
    ): Result<Spending> = runCatching {
        val request = SpendingRequest(
            amount = amount,
            categoryId = categoryId,
            subcategoryId = subCategoryId,
            description = description,
        )
        val remote = api.updateSpending(userEmail, id, request)
        dao.upsertSpending(remote.toEntity(synced = true))
        remote.toDomain()
    }.onFailure { Log.w(TAG, "updateSpending(id=$id) failed", it) }

    override suspend fun deleteSpending(userEmail: String, id: Long): Result<Unit> = runCatching {
        api.deleteSpending(userEmail, id)
        dao.deleteSpending(id)
    }.onFailure { Log.w(TAG, "deleteSpending(id=$id) failed", it) }

    private companion object {
        const val TAG = "SpendingRepository"
    }
}
