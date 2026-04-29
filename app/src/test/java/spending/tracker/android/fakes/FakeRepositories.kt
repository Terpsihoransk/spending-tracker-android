package spending.tracker.android.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import spending.tracker.android.domain.model.Category
import spending.tracker.android.domain.model.Spending
import spending.tracker.android.domain.model.SubCategory
import spending.tracker.android.domain.model.User
import spending.tracker.android.domain.repository.CategoryRepository
import spending.tracker.android.domain.repository.SpendingRepository
import spending.tracker.android.domain.repository.UserRepository
import java.time.LocalDate

/**
 * In-memory fake [SpendingRepository] для unit-тестов UseCases.
 * Сохраняет состояние в [MutableStateFlow], чтобы тесты могли подписываться через Turbine.
 */
class FakeSpendingRepository : SpendingRepository {
    private val state = MutableStateFlow<Map<String, List<Spending>>>(emptyMap())
    private var nextId: Long = 1
    var refreshCalls = 0
        private set
    var failAddWith: Throwable? = null
    var failUpdateWith: Throwable? = null
    var failDeleteWith: Throwable? = null

    fun seed(userEmail: String, spendings: List<Spending>) {
        state.value = state.value + (userEmail to spendings)
        nextId = (spendings.maxOfOrNull { it.id } ?: 0L) + 1
    }

    override fun observeSpendings(userEmail: String): Flow<List<Spending>> =
        state.asStateFlow().map { it[userEmail].orEmpty() }

    override suspend fun refreshSpendings(userEmail: String): Result<Unit> {
        refreshCalls++
        return Result.success(Unit)
    }

    override suspend fun addSpending(
        userEmail: String,
        amount: Double,
        categoryId: Long,
        subCategoryId: Long?,
        description: String?,
    ): Result<Spending> {
        failAddWith?.let { return Result.failure(it) }
        val newSpending = Spending(
            id = nextId++,
            amount = amount,
            categoryId = categoryId,
            categoryName = "Category-$categoryId",
            subCategoryId = subCategoryId,
            subCategoryName = subCategoryId?.let { "Sub-$it" },
            date = LocalDate.of(2025, 1, 1),
            description = description,
            userEmail = userEmail,
        )
        val current = state.value[userEmail].orEmpty()
        state.value = state.value + (userEmail to (current + newSpending))
        return Result.success(newSpending)
    }

    override suspend fun updateSpending(
        userEmail: String,
        id: Long,
        amount: Double,
        categoryId: Long,
        subCategoryId: Long?,
        description: String?,
    ): Result<Spending> {
        failUpdateWith?.let { return Result.failure(it) }
        val current = state.value[userEmail].orEmpty()
        val existing = current.firstOrNull { it.id == id }
            ?: return Result.failure(NoSuchElementException("spending $id not found"))
        val updated = existing.copy(
            amount = amount,
            categoryId = categoryId,
            subCategoryId = subCategoryId,
            description = description,
        )
        state.value = state.value + (userEmail to current.map { if (it.id == id) updated else it })
        return Result.success(updated)
    }

    override suspend fun deleteSpending(userEmail: String, id: Long): Result<Unit> {
        failDeleteWith?.let { return Result.failure(it) }
        val current = state.value[userEmail].orEmpty()
        state.value = state.value + (userEmail to current.filterNot { it.id == id })
        return Result.success(Unit)
    }
}

/** In-memory fake [CategoryRepository]. */
class FakeCategoryRepository : CategoryRepository {
    private val categories = MutableStateFlow<Map<String, List<Category>>>(emptyMap())
    private val subCategories = MutableStateFlow<Map<Long, List<SubCategory>>>(emptyMap())
    private var nextId: Long = 1

    var refreshCategoriesCalls = 0
        private set
    var refreshSubCategoriesCalls = 0
        private set
    var failAddCategoryWith: Throwable? = null

    override fun observeCategories(userEmail: String): Flow<List<Category>> =
        categories.map { it[userEmail].orEmpty() }

    override suspend fun refreshCategories(userEmail: String): Result<Unit> {
        refreshCategoriesCalls++
        return Result.success(Unit)
    }

    override suspend fun addCategory(userEmail: String, name: String): Result<Category> {
        failAddCategoryWith?.let { return Result.failure(it) }
        val cat = Category(id = nextId++, name = name, userEmail = userEmail)
        val current = categories.value[userEmail].orEmpty()
        categories.value = categories.value + (userEmail to (current + cat))
        return Result.success(cat)
    }

    override suspend fun updateCategory(userEmail: String, id: Long, name: String): Result<Category> {
        val current = categories.value[userEmail].orEmpty()
        val found = current.firstOrNull { it.id == id }
            ?: return Result.failure(NoSuchElementException("category $id"))
        val updated = found.copy(name = name)
        categories.value = categories.value +
                (userEmail to current.map { if (it.id == id) updated else it })
        return Result.success(updated)
    }

    override suspend fun deleteCategory(userEmail: String, id: Long): Result<Unit> {
        val current = categories.value[userEmail].orEmpty()
        categories.value = categories.value + (userEmail to current.filterNot { it.id == id })
        return Result.success(Unit)
    }

    override fun observeSubCategories(categoryId: Long): Flow<List<SubCategory>> =
        subCategories.map { it[categoryId].orEmpty() }

    override suspend fun refreshSubCategories(userEmail: String, categoryId: Long): Result<Unit> {
        refreshSubCategoriesCalls++
        return Result.success(Unit)
    }

    override suspend fun addSubCategory(
        userEmail: String,
        categoryId: Long,
        name: String,
    ): Result<SubCategory> {
        val sub = SubCategory(id = nextId++, name = name, categoryId = categoryId)
        val current = subCategories.value[categoryId].orEmpty()
        subCategories.value = subCategories.value + (categoryId to (current + sub))
        return Result.success(sub)
    }

    override suspend fun updateSubCategory(
        userEmail: String,
        id: Long,
        categoryId: Long,
        name: String,
    ): Result<SubCategory> {
        val current = subCategories.value[categoryId].orEmpty()
        val found = current.firstOrNull { it.id == id }
            ?: return Result.failure(NoSuchElementException("subcategory $id"))
        val updated = found.copy(name = name)
        subCategories.value = subCategories.value +
                (categoryId to current.map { if (it.id == id) updated else it })
        return Result.success(updated)
    }

    override suspend fun deleteSubCategory(userEmail: String, id: Long): Result<Unit> {
        // Находим категорию, где лежит эта sub, и удаляем из неё.
        val updated = subCategories.value.mapValues { (_, list) -> list.filterNot { it.id == id } }
        subCategories.value = updated
        return Result.success(Unit)
    }
}

/** In-memory fake [UserRepository]. */
class FakeUserRepository : UserRepository {
    private val current = MutableStateFlow<User?>(null)
    var syncCalls = 0
        private set
    var failSyncWith: Throwable? = null

    override fun observeCurrentUser(): Flow<User?> = current.asStateFlow()

    override suspend fun syncUser(email: String): Result<User> {
        syncCalls++
        failSyncWith?.let { return Result.failure(it) }
        val user = User(id = 1L, email = email, googleSheetsId = null)
        current.value = user
        return Result.success(user)
    }

    override suspend fun clearUser(): Result<Unit> {
        current.value = null
        return Result.success(Unit)
    }
}
