package spending.tracker.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import spending.tracker.android.domain.model.User
import spending.tracker.android.domain.repository.UserRepository

/** Подписка на текущего пользователя из локального кэша. */
class ObserveCurrentUserUseCase(private val repository: UserRepository) {
    operator fun invoke(): Flow<User?> = repository.observeCurrentUser()
}

/** Синхронизировать пользователя с бэкендом по email. */
class SyncUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(email: String): Result<User> = repository.syncUser(email)
}

/** Очистить данные пользователя (logout в будущем). */
class ClearUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(): Result<Unit> = repository.clearUser()
}
