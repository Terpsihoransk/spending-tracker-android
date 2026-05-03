package spending.tracker.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import spending.tracker.android.data.local.entity.CategoryEntity
import spending.tracker.android.data.local.entity.SubCategoryEntity

@Dao
interface CategoryDao {
    // --- Categories ---
    @Query("SELECT * FROM categories WHERE userEmail = :userEmail ORDER BY name ASC")
    fun observeCategories(userEmail: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE userEmail = :userEmail")
    suspend fun deleteCategoriesForUser(userEmail: String)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)

    /**
     * Удалить категории, которых нет в remote-списке.
     */
    @Query("DELETE FROM categories WHERE userEmail = :userEmail AND id NOT IN (:keepIds)")
    suspend fun deleteMissingCategoriesForUser(userEmail: String, keepIds: List<Long>)

    /**
     * Умный sync категорий.
     */
    @Transaction
    suspend fun syncCategoriesForUser(userEmail: String, remote: List<CategoryEntity>) {
        deleteMissingCategoriesForUser(userEmail, remote.map { it.id })
        upsertCategories(remote)
    }

    // --- SubCategories ---
    @Query("SELECT * FROM subcategories WHERE categoryId = :categoryId ORDER BY name ASC")
    fun observeSubCategories(categoryId: Long): Flow<List<SubCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubCategories(subCategories: List<SubCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubCategory(subCategory: SubCategoryEntity)

    @Query("DELETE FROM subcategories WHERE categoryId = :categoryId")
    suspend fun deleteSubCategoriesForCategory(categoryId: Long)

    @Query("DELETE FROM subcategories WHERE id = :id")
    suspend fun deleteSubCategory(id: Long)

    /**
     * Удалить подкатегории, которых нет в remote-списке.
     */
    @Query("DELETE FROM subcategories WHERE categoryId = :categoryId AND id NOT IN (:keepIds)")
    suspend fun deleteMissingSubCategoriesForCategory(categoryId: Long, keepIds: List<Long>)

    /**
     * Умный sync подкатегорий.
     */
    @Transaction
    suspend fun syncSubCategoriesForCategory(categoryId: Long, remote: List<SubCategoryEntity>) {
        deleteMissingSubCategoriesForCategory(categoryId, remote.map { it.id })
        upsertSubCategories(remote)
    }
}
