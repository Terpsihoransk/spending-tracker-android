package spending.tracker.android.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import spending.tracker.android.data.local.dao.CategoryDao
import spending.tracker.android.data.local.dao.SpendingDao
import spending.tracker.android.data.local.dao.UserDao
import spending.tracker.android.data.local.entity.CategoryEntity
import spending.tracker.android.data.local.entity.SpendingEntity
import spending.tracker.android.data.local.entity.SubCategoryEntity
import spending.tracker.android.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        SubCategoryEntity::class,
        SpendingEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun spendingDao(): SpendingDao
    abstract fun categoryDao(): CategoryDao
    abstract fun userDao(): UserDao

    companion object {
        const val DATABASE_NAME = "spending_tracker_db"
    }
}
