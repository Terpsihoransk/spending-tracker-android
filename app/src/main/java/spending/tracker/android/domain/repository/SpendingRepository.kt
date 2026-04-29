package spending.tracker.android.domain.repository

import kotlinx.coroutines.flow.Flow
import spending.tracker.android.domain.model.Spending

/**
 * Offline-first репозиторий расходов.
 *
 * UI подписывается на [observeSpendings] (источник — локальная БД Room),
 * а [refreshSpendings] дергается явно (pull-to-refresh, onStart экрана и т.п.),
 * чтобы синхронизировать данные с бэкендом.
 */
interface SpendingRepository {
    /** Подписка на локальный кэш расходов пользователя. */
    fun observeSpendings(userId: Long): Flow<List<Spending>>

    /** Принудительная синхронизация с бэкендом. Обновляет локальный кэш. */
    suspend fun refreshSpendings(userId: Long): Result<Unit>

    /** Добавить расход (сеть + локальный кэш). */
    suspend fun addSpending(spending: Spending): Result<Spending>

    /** Удалить расход по id (сеть + локальный кэш). */
    suspend fun deleteSpending(id: Long): Result<Unit>
}
