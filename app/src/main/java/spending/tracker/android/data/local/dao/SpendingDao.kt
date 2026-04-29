package spending.tracker.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import spending.tracker.android.data.local.entity.SpendingEntity

@Dao
interface SpendingDao {
    @Query("SELECT * FROM spendings WHERE userId = :userId ORDER BY date DESC, id DESC")
    fun observeSpendings(userId: Long): Flow<List<SpendingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSpending(spending: SpendingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSpendings(spendings: List<SpendingEntity>)

    @Query("DELETE FROM spendings WHERE id = :id")
    suspend fun deleteSpending(id: Long)

    @Query("DELETE FROM spendings WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Long)

    /**
     * Атомарно заменить весь список расходов пользователя на новый
     * (используется при refresh: сначала чистим локальный кэш, затем вставляем свежее).
     */
    @Transaction
    suspend fun replaceAllForUser(userId: Long, spendings: List<SpendingEntity>) {
        deleteAllForUser(userId)
        upsertSpendings(spendings)
    }
}
