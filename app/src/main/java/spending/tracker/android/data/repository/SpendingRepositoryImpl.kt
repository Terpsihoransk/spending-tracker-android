package spending.tracker.android.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import spending.tracker.android.data.local.dao.SpendingDao
import spending.tracker.android.data.remote.api.SpendingApi
import spending.tracker.android.domain.model.Spending
import spending.tracker.android.domain.repository.SpendingRepository
import spending.tracker.android.util.toCreateRequest
import spending.tracker.android.util.toDomain
import spending.tracker.android.util.toEntity

class SpendingRepositoryImpl(
    private val api: SpendingApi,
    private val dao: SpendingDao
) : SpendingRepository {

    override fun observeSpendings(userId: Long): Flow<List<Spending>> =
        dao.observeSpendings(userId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun refreshSpendings(userId: Long): Result<Unit> = runCatching {
        val remote = api.getSpendings(userId)
        dao.replaceAllForUser(userId, remote.map { it.toEntity(synced = true) })
    }.onFailure { Log.w(TAG, "refreshSpendings(userId=$userId) failed", it) }

    override suspend fun addSpending(spending: Spending): Result<Spending> = runCatching {
        val remote = api.createSpending(spending.toCreateRequest())
        val domain = remote.toDomain()
        dao.upsertSpending(domain.toEntity(synced = true))
        domain
    }.onFailure { Log.w(TAG, "addSpending failed", it) }

    override suspend fun deleteSpending(id: Long): Result<Unit> = runCatching {
        api.deleteSpending(id)
        dao.deleteSpending(id)
    }.onFailure { Log.w(TAG, "deleteSpending(id=$id) failed", it) }

    private companion object {
        const val TAG = "SpendingRepository"
    }
}
