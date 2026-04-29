package spending.tracker.android.domain.usecase

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import spending.tracker.android.fakes.FakeCategoryRepository

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryUseCasesTest {

    private lateinit var repo: FakeCategoryRepository
    private lateinit var observe: ObserveCategoriesUseCase
    private lateinit var refresh: RefreshCategoriesUseCase
    private lateinit var add: AddCategoryUseCase
    private lateinit var upd: UpdateCategoryUseCase
    private lateinit var del: DeleteCategoryUseCase
    private lateinit var observeSub: ObserveSubCategoriesUseCase
    private lateinit var addSub: AddSubCategoryUseCase
    private lateinit var updSub: UpdateSubCategoryUseCase
    private lateinit var delSub: DeleteSubCategoryUseCase

    private val email = "u@example.com"

    @Before
    fun setUp() {
        repo = FakeCategoryRepository()
        observe = ObserveCategoriesUseCase(repo)
        refresh = RefreshCategoriesUseCase(repo)
        add = AddCategoryUseCase(repo)
        upd = UpdateCategoryUseCase(repo)
        del = DeleteCategoryUseCase(repo)
        observeSub = ObserveSubCategoriesUseCase(repo)
        addSub = AddSubCategoryUseCase(repo)
        updSub = UpdateSubCategoryUseCase(repo)
        delSub = DeleteSubCategoryUseCase(repo)
    }

    @Test
    fun `AddCategoryUseCase creates category and it appears in observe`() = runTest {
        val created = add(email, "Еда").getOrThrow()
        assertEquals("Еда", created.name)

        observe(email).test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("Еда", items[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `UpdateCategoryUseCase changes name`() = runTest {
        val created = add(email, "Еда").getOrThrow()
        val updated = upd(email, created.id, "Продукты").getOrThrow()
        assertEquals("Продукты", updated.name)
    }

    @Test
    fun `DeleteCategoryUseCase removes category`() = runTest {
        val created = add(email, "Тест").getOrThrow()
        assertTrue(del(email, created.id).isSuccess)
        observe(email).test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `AddCategoryUseCase propagates failure from repository`() = runTest {
        repo.failAddCategoryWith = RuntimeException("boom")
        val r = add(email, "X")
        assertTrue(r.isFailure)
        assertEquals("boom", r.exceptionOrNull()?.message)
    }

    @Test
    fun `RefreshCategoriesUseCase delegates to repository`() = runTest {
        assertEquals(0, repo.refreshCategoriesCalls)
        refresh(email).getOrThrow()
        assertEquals(1, repo.refreshCategoriesCalls)
    }

    @Test
    fun `SubCategory CRUD works end-to-end`() = runTest {
        val cat = add(email, "Еда").getOrThrow()

        val sub = addSub(email, cat.id, "Кафе").getOrThrow()
        assertEquals("Кафе", sub.name)
        assertEquals(cat.id, sub.categoryId)

        observeSub(cat.id).test {
            assertEquals(listOf(sub), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        val updatedSub = updSub(email, sub.id, cat.id, "Рестораны").getOrThrow()
        assertEquals("Рестораны", updatedSub.name)

        assertTrue(delSub(email, sub.id).isSuccess)
        observeSub(cat.id).test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `RefreshSubCategoriesUseCase delegates`() = runTest {
        val refreshSub = RefreshSubCategoriesUseCase(repo)
        refreshSub(email, categoryId = 42).getOrThrow()
        assertEquals(1, repo.refreshSubCategoriesCalls)
    }
}
