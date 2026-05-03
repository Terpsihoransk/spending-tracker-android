package spending.tracker.android.domain.usecase

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import spending.tracker.android.domain.model.Spending
import spending.tracker.android.fakes.FakeSpendingRepository
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SpendingUseCasesTest {

    private lateinit var repo: FakeSpendingRepository
    private lateinit var observe: ObserveSpendingsUseCase
    private lateinit var refresh: RefreshSpendingsUseCase
    private lateinit var add: AddSpendingUseCase
    private lateinit var update: UpdateSpendingUseCase
    private lateinit var delete: DeleteSpendingUseCase

    private val email = "user@example.com"

    @Before
    fun setUp() {
        repo = FakeSpendingRepository()
        observe = ObserveSpendingsUseCase(repo)
        refresh = RefreshSpendingsUseCase(repo)
        add = AddSpendingUseCase(repo)
        update = UpdateSpendingUseCase(repo)
        delete = DeleteSpendingUseCase(repo)
    }

    @Test
    fun `ObserveSpendingsUseCase emits empty list by default`() = runTest {
        observe(email).test {
            assertEquals(emptyList<Spending>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ObserveSpendingsUseCase emits seeded data`() = runTest {
        val seed = Spending(
            id = 42,
            amount = BigDecimal("100.0"),
            categoryId = 1,
            categoryName = "Еда",
            subCategoryId = null,
            subCategoryName = null,
            date = LocalDate.of(2025, 1, 15),
            description = null,
            userEmail = email,
        )
        repo.seed(email, listOf(seed))

        observe(email).test {
            assertEquals(listOf(seed), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `AddSpendingUseCase creates a new spending and it appears in observe`() = runTest {
        val result = add(email, amount = BigDecimal("250.0"), categoryId = 10, subCategoryId = null, description = "lunch")

        assertTrue(result.isSuccess)
        val created = result.getOrNull()!!
        assertEquals(BigDecimal("250.0"), created.amount)
        assertEquals(10L, created.categoryId)
        assertEquals("lunch", created.description)

        observe(email).test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals(created.id, items[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `AddSpendingUseCase propagates repository failure`() = runTest {
        repo.failAddWith = IllegalStateException("network down")
        val result = add(email, amount = BigDecimal.ONE, categoryId = 1, subCategoryId = null, description = null)
        assertTrue(result.isFailure)
        assertEquals("network down", result.exceptionOrNull()?.message)
    }

    @Test
    fun `UpdateSpendingUseCase changes amount and category`() = runTest {
        val created = add(email, BigDecimal("100.0"), categoryId = 1, subCategoryId = null, description = null).getOrThrow()

        val updated = update(
            userEmail = email,
            id = created.id,
            amount = BigDecimal("500.0"),
            categoryId = 2,
            subCategoryId = null,
            description = "updated",
        ).getOrThrow()

        assertEquals(BigDecimal("500.0"), updated.amount)
        assertEquals(2L, updated.categoryId)
        assertEquals("updated", updated.description)
    }

    @Test
    fun `UpdateSpendingUseCase fails for missing id`() = runTest {
        val result = update(email, id = 999, amount = BigDecimal.ONE, categoryId = 1, subCategoryId = null, description = null)
        assertTrue(result.isFailure)
    }

    @Test
    fun `DeleteSpendingUseCase removes spending`() = runTest {
        val created = add(email, BigDecimal("100.0"), 1, null, null).getOrThrow()
        assertTrue(delete(email, created.id).isSuccess)

        observe(email).test {
            assertEquals(emptyList<Spending>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `RefreshSpendingsUseCase delegates to repository`() = runTest {
        assertEquals(0, repo.refreshCalls)
        val res = refresh(email)
        assertTrue(res.isSuccess)
        assertEquals(1, repo.refreshCalls)
    }

    @Test
    fun `observe emits update on add`() = runTest {
        observe(email).test {
            // Начальное пустое значение.
            assertEquals(emptyList<Spending>(), awaitItem())
            add(email, BigDecimal("10.0"), 1, null, null).getOrThrow()
            val afterAdd = awaitItem()
            assertEquals(1, afterAdd.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observe for different user is isolated`() = runTest {
        add(email, BigDecimal("100.0"), 1, null, null).getOrThrow()
        observe("other@example.com").test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
        observe(email).test {
            assertFalse(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `created spending has non-null id`() = runTest {
        val first = add(email, BigDecimal("10.0"), 1, null, null).getOrThrow()
        val second = add(email, BigDecimal("20.0"), 1, null, null).getOrThrow()
        assertNotNull(first.id)
        assertNotNull(second.id)
        assertTrue(second.id > first.id)
        assertNull(first.subCategoryId)
    }
}
