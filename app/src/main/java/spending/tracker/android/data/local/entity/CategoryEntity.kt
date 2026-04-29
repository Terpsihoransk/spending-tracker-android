package spending.tracker.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index("userId")]
)
data class CategoryEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val userId: Long
)
