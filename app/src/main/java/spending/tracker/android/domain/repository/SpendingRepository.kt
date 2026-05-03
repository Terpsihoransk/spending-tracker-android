package spending.tracker.android.domain.repository

import kotlinx.coroutines.flow.Flow
import spending.tracker.android.domain.model.Spending
import java.math.BigDecimal

/**
 * Offline-first репозиторий расходов.
 *
 * UI подписывается на [observeSpendings] (источник — локальная БД Room),
 * а [refreshSpendings] дергается явно (pull-to-refresh, onStart экрана и т.п.),
 * чтобы синхронизировать данные с бэкендом.
 */
interface SpendingRepository {
    /** Подписка на локальный кэш расходов пользователя. */
    fun observeSpendings(userEmail: String): Flow<List<Spending>>

    /** Принудительная синхронизация с бэкендом. Обновляет локальный кэш. */
    suspend fun refreshSpendings(userEmail: String): Result<Unit>

    /** Получить один расход по ID с бэка. */
    suspend fun getSpendingById(userEmail: String, id: Long): Result<Spending>

    /**
     * Добавить расход (сеть + локальный кэш).
     *
     * На бэке `date` проставляется серверной стороной в момент создания,
     * поэтому сюда поле даты не передаём.
     */
    suspend fun addSpending(
        userEmail: String,
        amount: BigDecimal,
        categoryId: Long,
        subCategoryId: Long?,
        description: String?,
    ): Result<Spending>

    /** Обновить существующий расход. */
    suspend fun updateSpending(
        userEmail: String,
        id: Long,
        amount: BigDecimal,
        categoryId: Long,
        subCategoryId: Long?,
        description: String?,
    ): Result<Spending>

    /** Удалить расход по id (сеть + локальный кэш). */
    suspend fun deleteSpending(userEmail: String, id: Long): Result<Unit>
}
