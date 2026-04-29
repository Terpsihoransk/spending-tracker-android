package spending.tracker.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import spending.tracker.android.domain.model.Spending
import spending.tracker.android.domain.repository.SpendingRepository

/** Подписка на список расходов пользователя (из локального кэша). */
class ObserveSpendingsUseCase(private val repository: SpendingRepository) {
    operator fun invoke(userEmail: String): Flow<List<Spending>> =
        repository.observeSpendings(userEmail)
}

/** Принудительно обновить список расходов из сети. */
class RefreshSpendingsUseCase(private val repository: SpendingRepository) {
    suspend operator fun invoke(userEmail: String): Result<Unit> =
        repository.refreshSpendings(userEmail)
}

/** Добавить расход. Дата выставляется сервером. */
class AddSpendingUseCase(private val repository: SpendingRepository) {
    suspend operator fun invoke(
        userEmail: String,
        amount: Double,
        categoryId: Long,
        subCategoryId: Long?,
        description: String?,
    ): Result<Spending> = repository.addSpending(userEmail, amount, categoryId, subCategoryId, description)
}

/** Обновить существующий расход. */
class UpdateSpendingUseCase(private val repository: SpendingRepository) {
    suspend operator fun invoke(
        userEmail: String,
        id: Long,
        amount: Double,
        categoryId: Long,
        subCategoryId: Long?,
        description: String?,
    ): Result<Spending> = repository.updateSpending(userEmail, id, amount, categoryId, subCategoryId, description)
}

/** Удалить расход. */
class DeleteSpendingUseCase(private val repository: SpendingRepository) {
    suspend operator fun invoke(userEmail: String, id: Long): Result<Unit> =
        repository.deleteSpending(userEmail, id)
}
