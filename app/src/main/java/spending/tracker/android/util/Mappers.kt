package spending.tracker.android.util

import spending.tracker.android.data.local.entity.CategoryEntity
import spending.tracker.android.data.local.entity.SpendingEntity
import spending.tracker.android.data.local.entity.SubCategoryEntity
import spending.tracker.android.data.local.entity.UserEntity
import spending.tracker.android.data.remote.dto.CategoryResponse
import spending.tracker.android.data.remote.dto.SpendingResponse
import spending.tracker.android.data.remote.dto.SubCategoryResponse
import spending.tracker.android.data.remote.dto.UserResponse
import spending.tracker.android.domain.model.Category
import spending.tracker.android.domain.model.Spending
import spending.tracker.android.domain.model.SubCategory
import spending.tracker.android.domain.model.User
import java.math.BigDecimal

// ============ User ============
fun UserResponse.toDomain() = User(
    id = id,
    email = email,
    googleSheetsId = googleSheetsId,
)

fun UserResponse.toEntity() = UserEntity(
    id = id,
    email = email,
    googleSheetsId = googleSheetsId,
)

fun UserEntity.toDomain() = User(
    id = id,
    email = email,
    googleSheetsId = googleSheetsId,
)

// ============ Spending ============
/**
 * Парсит строку в BigDecimal.
 * При невалидном значении выбрасывает [NumberFormatException],
 * который будет пойман [runCatching] в репозитории.
 */
private fun String.parseAmount(): BigDecimal = BigDecimal(this)

fun SpendingResponse.toDomain() = Spending(
    id = id,
    amount = amount.parseAmount(),
    categoryId = categoryId,
    categoryName = categoryName,
    subCategoryId = subcategoryId,
    subCategoryName = subcategoryName,
    date = date,
    description = description,
    userEmail = userEmail,
)

fun SpendingResponse.toEntity(synced: Boolean = true) = SpendingEntity(
    id = id,
    amount = amount, // уже строка от бэка
    categoryId = categoryId,
    categoryName = categoryName,
    subCategoryId = subcategoryId,
    subCategoryName = subcategoryName,
    date = date.toIsoString(),
    description = description,
    userEmail = userEmail,
    synced = synced,
)

fun SpendingEntity.toDomain() = Spending(
    id = id,
    amount = amount.parseAmount(),
    categoryId = categoryId,
    categoryName = categoryName,
    subCategoryId = subCategoryId,
    subCategoryName = subCategoryName,
    date = date.toLocalDate(),
    description = description,
    userEmail = userEmail,
)

fun Spending.toEntity(synced: Boolean = true) = SpendingEntity(
    id = id,
    amount = amount.toPlainString(),
    categoryId = categoryId,
    categoryName = categoryName,
    subCategoryId = subCategoryId,
    subCategoryName = subCategoryName,
    date = date.toIsoString(),
    description = description,
    userEmail = userEmail,
    synced = synced,
)

// ============ Category ============
fun CategoryResponse.toDomain() = Category(id = id, name = name, userEmail = userEmail)
fun CategoryResponse.toEntity() = CategoryEntity(id = id, name = name, userEmail = userEmail)
fun CategoryEntity.toDomain() = Category(id = id, name = name, userEmail = userEmail)

// ============ SubCategory ============
fun SubCategoryResponse.toDomain() = SubCategory(id = id, name = name, categoryId = categoryId)
fun SubCategoryResponse.toEntity() = SubCategoryEntity(id = id, name = name, categoryId = categoryId)
fun SubCategoryEntity.toDomain() = SubCategory(id = id, name = name, categoryId = categoryId)
