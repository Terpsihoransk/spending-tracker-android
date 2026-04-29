package spending.tracker.android.util

import spending.tracker.android.data.local.entity.CategoryEntity
import spending.tracker.android.data.local.entity.SpendingEntity
import spending.tracker.android.data.local.entity.SubCategoryEntity
import spending.tracker.android.data.local.entity.UserEntity
import spending.tracker.android.data.remote.dto.CategoryDto
import spending.tracker.android.data.remote.dto.CreateSpendingRequest
import spending.tracker.android.data.remote.dto.SpendingDto
import spending.tracker.android.data.remote.dto.SubCategoryDto
import spending.tracker.android.data.remote.dto.UserDto
import spending.tracker.android.domain.model.Category
import spending.tracker.android.domain.model.Spending
import spending.tracker.android.domain.model.SubCategory
import spending.tracker.android.domain.model.User

// ============ User ============
fun UserDto.toDomain() = User(
    id = id,
    email = email,
    googleSheetsId = googleSheetsId
)

fun UserDto.toEntity() = UserEntity(
    id = id,
    email = email,
    googleSheetsId = googleSheetsId
)

fun UserEntity.toDomain() = User(
    id = id,
    email = email,
    googleSheetsId = googleSheetsId
)

// ============ Spending ============
fun SpendingDto.toDomain() = Spending(
    id = id,
    amount = amount,
    categoryId = categoryId,
    subCategoryId = subCategoryId,
    date = date,
    description = description,
    userId = userId
)

fun SpendingDto.toEntity(synced: Boolean = true) = SpendingEntity(
    id = id,
    amount = amount,
    categoryId = categoryId,
    subCategoryId = subCategoryId,
    date = date.toIsoString(),
    description = description,
    userId = userId,
    synced = synced
)

fun SpendingEntity.toDomain() = Spending(
    id = id,
    amount = amount,
    categoryId = categoryId,
    subCategoryId = subCategoryId,
    date = date.toLocalDate(),
    description = description,
    userId = userId
)

fun Spending.toEntity(synced: Boolean = true) = SpendingEntity(
    id = id,
    amount = amount,
    categoryId = categoryId,
    subCategoryId = subCategoryId,
    date = date.toIsoString(),
    description = description,
    userId = userId,
    synced = synced
)

fun Spending.toCreateRequest() = CreateSpendingRequest(
    amount = amount,
    categoryId = categoryId,
    subCategoryId = subCategoryId,
    date = date,
    description = description,
    userId = userId
)

// ============ Category ============
fun CategoryDto.toDomain() = Category(id = id, name = name, userId = userId)
fun CategoryDto.toEntity() = CategoryEntity(id = id, name = name, userId = userId)
fun CategoryEntity.toDomain() = Category(id = id, name = name, userId = userId)

// ============ SubCategory ============
fun SubCategoryDto.toDomain() = SubCategory(id = id, name = name, categoryId = categoryId)
fun SubCategoryDto.toEntity() = SubCategoryEntity(id = id, name = name, categoryId = categoryId)
fun SubCategoryEntity.toDomain() = SubCategory(id = id, name = name, categoryId = categoryId)
