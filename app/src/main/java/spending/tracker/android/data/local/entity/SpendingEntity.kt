package spending.tracker.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "spendings",
    indices = [
        Index("userEmail"),
        Index("categoryId"),
        Index("subCategoryId"),
        Index("date")
    ]
)
data class SpendingEntity(
    @PrimaryKey val id: Long,
    val amount: Double,
    val categoryId: Long,
    val categoryName: String,
    val subCategoryId: Long?,
    val subCategoryName: String?,
    val date: String, // ISO-8601 (LocalDate.toString())
    val description: String?,
    val userEmail: String,
    val synced: Boolean = true,
)
