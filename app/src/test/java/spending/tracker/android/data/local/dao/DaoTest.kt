package spending.tracker.android.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import spending.tracker.android.data.local.database.AppDatabase
import spending.tracker.android.data.local.entity.CategoryEntity
import spending.tracker.android.data.local.entity.SpendingEntity
import spending.tracker.android.data.local.entity.SubCategoryEntity
import spending.tracker.android.data.local.entity.UserEntity

/**
 * Интеграционные тесты Room-DAO через in-memory БД на JVM (Robolectric).
 *
 * Покрывают:
 *  - `UserDao`: upsert / replaceCurrentUser / clearUsers / observeCurrentUser
 *  - `CategoryDao`: CRUD, replace, FK-согласованность для SubCategory
 *  - `SpendingDao`: CRUD, replaceAllForUser, сортировка
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    manifest = Config.NONE,
    application = spending.tracker.android.TestApplication::class,
)
@OptIn(ExperimentalCoroutinesApi::class)
class DaoTest {

    private lateinit var db: AppDatabase
    private lateinit var spendingDao: SpendingDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var userDao: UserDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        spendingDao = db.spendingDao()
        categoryDao = db.categoryDao()
        userDao = db.userDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // ---------- UserDao ----------

    @Test
    fun userDao_upsertAndObserve() = runTest {
        assertNull(userDao.observeCurrentUser().first())
        val user = UserEntity(id = 1, email = "a@b.c", googleSheetsId = null)
        userDao.upsertUser(user)
        assertEquals(user, userDao.observeCurrentUser().first())
        assertEquals(user, userDao.getUserByEmail("a@b.c"))
        assertNull(userDao.getUserByEmail("none@x.y"))
    }

    @Test
    fun userDao_replaceCurrentUser_keepsOnlyOneRow() = runTest {
        userDao.upsertUser(UserEntity(1, "a@b.c", null))
        userDao.replaceCurrentUser(UserEntity(2, "b@c.d", "sheet-id"))
        val current = userDao.observeCurrentUser().first()
        assertNotNull(current)
        assertEquals("b@c.d", current!!.email)
        // только один ряд
        assertNull(userDao.getUserByEmail("a@b.c"))
    }

    @Test
    fun userDao_clearUsers_removesAll() = runTest {
        userDao.upsertUser(UserEntity(1, "a@b.c", null))
        userDao.clearUsers()
        assertNull(userDao.observeCurrentUser().first())
    }

    // ---------- CategoryDao ----------

    @Test
    fun categoryDao_upsertAndObserve() = runTest {
        val email = "u@x.y"
        categoryDao.upsertCategory(CategoryEntity(id = 1, name = "B", userEmail = email))
        categoryDao.upsertCategory(CategoryEntity(id = 2, name = "A", userEmail = email))

        val list = categoryDao.observeCategories(email).first()
        assertEquals(2, list.size)
        // По ORDER BY name ASC — сначала "A", потом "B".
        assertEquals("A", list[0].name)
        assertEquals("B", list[1].name)
    }

    @Test
    fun categoryDao_replaceCategoriesForUser_clearsOldAndInsertsNew() = runTest {
        val email = "u@x.y"
        categoryDao.upsertCategory(CategoryEntity(1, "Old", email))
        categoryDao.replaceCategoriesForUser(
            email,
            listOf(
                CategoryEntity(10, "Fresh-1", email),
                CategoryEntity(11, "Fresh-2", email),
            ),
        )
        val after = categoryDao.observeCategories(email).first()
        assertEquals(2, after.size)
        assertTrue(after.all { it.name.startsWith("Fresh") })
    }

    @Test
    fun categoryDao_subCategoriesFilteredByCategoryId() = runTest {
        val email = "u@x.y"
        categoryDao.upsertCategory(CategoryEntity(1, "Food", email))
        categoryDao.upsertCategory(CategoryEntity(2, "Transport", email))
        categoryDao.upsertSubCategory(SubCategoryEntity(10, "Pizza", 1))
        categoryDao.upsertSubCategory(SubCategoryEntity(11, "Taxi", 2))
        categoryDao.upsertSubCategory(SubCategoryEntity(12, "Bus", 2))

        val foodSubs = categoryDao.observeSubCategories(1).first()
        assertEquals(listOf("Pizza"), foodSubs.map { it.name })

        val transportSubs = categoryDao.observeSubCategories(2).first()
        assertEquals(2, transportSubs.size)
    }

    @Test
    fun categoryDao_deleteCategory_removesOnlyThatOne() = runTest {
        val email = "u@x.y"
        categoryDao.upsertCategories(
            listOf(
                CategoryEntity(1, "A", email),
                CategoryEntity(2, "B", email),
            ),
        )
        categoryDao.deleteCategory(1)
        val left = categoryDao.observeCategories(email).first()
        assertEquals(listOf(2L), left.map { it.id })
    }

    // ---------- SpendingDao ----------

    @Test
    fun spendingDao_upsertAndObserve_ordersByDateDesc() = runTest {
        val email = "u@x.y"
        spendingDao.upsertSpending(
            SpendingEntity(
                id = 1, amount = 100.0, categoryId = 1, categoryName = "Food",
                subCategoryId = null, subCategoryName = null,
                date = "2025-01-01", description = null, userEmail = email, synced = true,
            ),
        )
        spendingDao.upsertSpending(
            SpendingEntity(
                id = 2, amount = 200.0, categoryId = 1, categoryName = "Food",
                subCategoryId = null, subCategoryName = null,
                date = "2025-02-01", description = null, userEmail = email, synced = true,
            ),
        )
        val list = spendingDao.observeSpendings(email).first()
        // Более новая дата 2025-02-01 должна идти первой.
        assertEquals(2L, list[0].id)
        assertEquals(1L, list[1].id)
    }

    @Test
    fun spendingDao_replaceAllForUser_removesOldSpendings() = runTest {
        val email = "u@x.y"
        spendingDao.upsertSpending(
            SpendingEntity(
                id = 1, amount = 100.0, categoryId = 1, categoryName = "Food",
                subCategoryId = null, subCategoryName = null,
                date = "2025-01-01", description = null, userEmail = email, synced = true,
            ),
        )
        spendingDao.replaceAllForUser(
            email,
            listOf(
                SpendingEntity(
                    id = 99, amount = 10.0, categoryId = 1, categoryName = "Food",
                    subCategoryId = null, subCategoryName = null,
                    date = "2025-03-01", description = null, userEmail = email, synced = true,
                ),
            ),
        )
        val list = spendingDao.observeSpendings(email).first()
        assertEquals(1, list.size)
        assertEquals(99L, list[0].id)
    }

    @Test
    fun spendingDao_isolatedByUserEmail() = runTest {
        spendingDao.upsertSpending(
            SpendingEntity(
                id = 1, amount = 1.0, categoryId = 1, categoryName = "C",
                subCategoryId = null, subCategoryName = null,
                date = "2025-01-01", description = null, userEmail = "a@a", synced = true,
            ),
        )
        spendingDao.upsertSpending(
            SpendingEntity(
                id = 2, amount = 2.0, categoryId = 1, categoryName = "C",
                subCategoryId = null, subCategoryName = null,
                date = "2025-01-01", description = null, userEmail = "b@b", synced = true,
            ),
        )
        assertEquals(1, spendingDao.observeSpendings("a@a").first().size)
        assertEquals(1, spendingDao.observeSpendings("b@b").first().size)
        assertEquals(0, spendingDao.observeSpendings("c@c").first().size)
    }

    @Test
    fun spendingDao_deleteSpending() = runTest {
        val email = "u@x.y"
        spendingDao.upsertSpending(
            SpendingEntity(
                id = 1, amount = 10.0, categoryId = 1, categoryName = "C",
                subCategoryId = null, subCategoryName = null,
                date = "2025-01-01", description = null, userEmail = email, synced = true,
            ),
        )
        spendingDao.deleteSpending(1)
        assertTrue(spendingDao.observeSpendings(email).first().isEmpty())
    }
}
