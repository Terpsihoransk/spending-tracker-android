package spending.tracker.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import spending.tracker.android.domain.model.Spending
import spending.tracker.android.domain.repository.SpendingRepository

/** Подписка на список расходов пользователя (из локального кэша). */
class ObserveSpendingsUseCase(private val repository: SpendingRepository) {
    operator fun invoke(userId: Long): Flow<List<Spending>> = repository.observeSpendings(userId)
}

/** Принудительно обновить список расходов из сети. */
class RefreshSpendingsUseCase(private val repository: SpendingRepository) {
    suspend operator fun invoke(userId: Long): Result<Unit> = repository.refreshSpendings(userId)
}

/** Удалить расход. */
class DeleteSpendingUseCase(private val repository: SpendingRepository) {
    suspend operator fun invoke(id: Long): Result<Unit> = repository.deleteSpending(id)
}
