package spending.tracker.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import spending.tracker.android.data.local.entity.UserEntity

@Dao
interface UserDao {
    /** Текущий пользователь (MVP1: только один активный). */
    @Query("SELECT * FROM users LIMIT 1")
    fun observeCurrentUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clearUsers()

    /**
     * Заменить текущего пользователя (чистим таблицу и кладём нового).
     * MVP1: один пользователь = один ряд.
     */
    @Transaction
    suspend fun replaceCurrentUser(user: UserEntity) {
        clearUsers()
        upsertUser(user)
    }
}
