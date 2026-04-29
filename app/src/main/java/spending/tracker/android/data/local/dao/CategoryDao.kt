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
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name ASC")
    fun observeCategories(userId: Long): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE userId = :userId")
    suspend fun deleteCategoriesForUser(userId: Long)

    @Transaction
    suspend fun replaceCategoriesForUser(userId: Long, categories: List<CategoryEntity>) {
        deleteCategoriesForUser(userId)
        upsertCategories(categories)
    }

    // --- SubCategories ---
    @Query("SELECT * FROM subcategories WHERE categoryId = :categoryId ORDER BY name ASC")
    fun observeSubCategories(categoryId: Long): Flow<List<SubCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubCategories(subCategories: List<SubCategoryEntity>)

    @Query("DELETE FROM subcategories WHERE categoryId = :categoryId")
    suspend fun deleteSubCategoriesForCategory(categoryId: Long)

    @Transaction
    suspend fun replaceSubCategoriesForCategory(categoryId: Long, subCategories: List<SubCategoryEntity>) {
        deleteSubCategoriesForCategory(categoryId)
        upsertSubCategories(subCategories)
    }
}
