package spending.tracker.android.domain.model

import java.math.BigDecimal
import java.time.LocalDate

data class Spending(
    val id: Long,
    val amount: BigDecimal,
    val categoryId: Long,
    val categoryName: String,
    val subCategoryId: Long?,
    val subCategoryName: String?,
    val date: LocalDate,
    val description: String?,
    val userEmail: String,
)
