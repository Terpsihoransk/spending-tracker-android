package spending.tracker.android.data.remote.dto

import kotlinx.serialization.Serializable
import spending.tracker.android.util.LocalDateSerializer
import java.time.LocalDate

// ============ User ============

@Serializable
data class UserRequest(
    val email: String,
    val googleSheetsId: String? = null,
)

@Serializable
data class UserResponse(
    val id: Long,
    val email: String,
    val googleSheetsId: String? = null,
)

// ============ Category ============

@Serializable
data class CategoryRequest(
    val name: String,
)

@Serializable
data class CategoryResponse(
    val id: Long,
    val name: String,
    val userEmail: String,
)

// ============ SubCategory ============

@Serializable
data class SubCategoryRequest(
    val name: String,
    val categoryId: Long,
)

@Serializable
data class SubCategoryResponse(
    val id: Long,
    val name: String,
    val categoryId: Long,
    val categoryName: String? = null,
)

// ============ Spending ============

@Serializable
data class SpendingRequest(
    val amount: Double,
    val categoryId: Long,
    val subcategoryId: Long? = null,
    val description: String? = null,
)

@Serializable
data class SpendingResponse(
    val id: Long,
    /** На бэке BigDecimal сериализуется как строка. */
    val amount: String,
    val categoryId: Long,
    val categoryName: String,
    val subcategoryId: Long? = null,
    val subcategoryName: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val description: String? = null,
    val userEmail: String,
)
