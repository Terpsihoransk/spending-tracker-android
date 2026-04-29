package spending.tracker.android.data.remote.dto

import kotlinx.serialization.Serializable
import spending.tracker.android.util.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class CategoryDto(
    val id: Long,
    val name: String,
    val userId: Long
)

@Serializable
data class SubCategoryDto(
    val id: Long,
    val name: String,
    val categoryId: Long
)

@Serializable
data class SpendingDto(
    val id: Long,
    val amount: Double,
    val categoryId: Long,
    val subCategoryId: Long?,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val description: String?,
    val userId: Long
)

@Serializable
data class CreateSpendingRequest(
    val amount: Double,
    val categoryId: Long,
    val subCategoryId: Long?,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val description: String?,
    val userId: Long
)

@Serializable
data class UserDto(
    val id: Long,
    val email: String,
    val googleSheetsId: String?
)
