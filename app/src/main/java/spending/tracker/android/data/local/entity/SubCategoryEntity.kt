package spending.tracker.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subcategories",
    indices = [Index("categoryId")]
)
data class SubCategoryEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val categoryId: Long
)
