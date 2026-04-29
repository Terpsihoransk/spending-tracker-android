package spending.tracker.android.domain.model

import java.time.LocalDate

data class Spending(
    val id: Long,
    val amount: Double,
    val categoryId: Long,
    val subCategoryId: Long?,
    val date: LocalDate,
    val description: String?,
    val userId: Long
)
