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
    @Query("SELECT * FROM spendings WHERE userEmail = :userEmail ORDER BY date DESC, id DESC")
    fun observeSpendings(userEmail: String): Flow<List<SpendingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSpending(spending: SpendingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSpendings(spendings: List<SpendingEntity>)

    @Query("DELETE FROM spendings WHERE id = :id")
    suspend fun deleteSpending(id: Long)

    @Query("DELETE FROM spendings WHERE userEmail = :userEmail")
    suspend fun deleteAllForUser(userEmail: String)

    /**
     * Атомарно заменить весь список расходов пользователя на новый
     * (используется при refresh: сначала чистим локальный кэш, затем вставляем свежее).
     */
    @Transaction
    suspend fun replaceAllForUser(userEmail: String, spendings: List<SpendingEntity>) {
        deleteAllForUser(userEmail)
        upsertSpendings(spendings)
    }
}
