package spending.tracker.android.domain.repository

import kotlinx.coroutines.flow.Flow
import spending.tracker.android.domain.model.User

/**
 * Offline-first репозиторий пользователя (MVP1: один пользователь на устройстве).
 *
 * [observeCurrentUser] — подписка на локальный кэш (источник истины для UI).
 * [syncUser] — запросить пользователя с бэкенда по email и обновить локальный кэш.
 */
interface UserRepository {
    fun observeCurrentUser(): Flow<User?>
    suspend fun syncUser(email: String): Result<User>
    suspend fun clearUser(): Result<Unit>
}
