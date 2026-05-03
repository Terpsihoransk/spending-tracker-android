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
     * Удалить записи, которых нет в remote-списке.
     * Это позволяет избежать промежуточного пустого состояния в Flow
     * при sync (delete -> insert вместо delete all -> insert all).
     */
    @Query("DELETE FROM spendings WHERE userEmail = :userEmail AND id NOT IN (:keepIds)")
    suspend fun deleteMissingForUser(userEmail: String, keepIds: List<Long>)

    /**
     * Умный sync: удаляем только отсутствующие в remote, затем upsert'им всё.
     * Не вызывает промежуточного пустого эмита в observe-Flow.
     */
    @Transaction
    suspend fun syncForUser(userEmail: String, remote: List<SpendingEntity>) {
        deleteMissingForUser(userEmail, remote.map { it.id })
        upsertSpendings(remote)
    }
}
