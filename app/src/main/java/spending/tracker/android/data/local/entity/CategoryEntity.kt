package spending.tracker.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index("userEmail")]
)
data class CategoryEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val userEmail: String,
)
