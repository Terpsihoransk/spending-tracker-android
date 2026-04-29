package spending.tracker.android.data.repository

import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import spending.tracker.android.data.local.database.AppDatabase
import spending.tracker.android.data.local.dao.UserDao
import spending.tracker.android.data.remote.api.UserApi
import spending.tracker.android.data.remote.dto.UserRequest
import spending.tracker.android.domain.model.User
import spending.tracker.android.domain.repository.UserRepository
import spending.tracker.android.util.toDomain
import spending.tracker.android.util.toEntity

class UserRepositoryImpl(
    private val api: UserApi,
    private val dao: UserDao,
    private val appDatabase: AppDatabase,
) : UserRepository {

    override fun observeCurrentUser(): Flow<User?> =
        dao.observeCurrentUser().map { it?.toDomain() }

    override suspend fun syncUser(email: String): Result<User> = runCatching {
        val existing = api.getUserByEmail(email)
        val remote = existing ?: api.createUser(UserRequest(email = email, googleSheetsId = null))
        dao.replaceCurrentUser(remote.toEntity())
        remote.toDomain()
    }.onFailure { Log.w(TAG, "syncUser(email=$email) failed", it) }

    override suspend fun clearUser(): Result<Unit> = runCatching {
        // Если clearAllTables вызывается во время активной синхронизации — данные не просочатся частично.
        appDatabase.withTransaction {
            appDatabase.clearAllTables()
        }
    }.onFailure { Log.w(TAG, "clearUser failed", it) }

    private companion object {
        const val TAG = "UserRepository"
    }
}
