package spending.tracker.android.domain.usecase

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import spending.tracker.android.fakes.FakeUserRepository

@OptIn(ExperimentalCoroutinesApi::class)
class UserUseCasesTest {

    private lateinit var repo: FakeUserRepository
    private lateinit var observe: ObserveCurrentUserUseCase
    private lateinit var sync: SyncUserUseCase
    private lateinit var clear: ClearUserUseCase

    @Before
    fun setUp() {
        repo = FakeUserRepository()
        observe = ObserveCurrentUserUseCase(repo)
        sync = SyncUserUseCase(repo)
        clear = ClearUserUseCase(repo)
    }

    @Test
    fun `ObserveCurrentUserUseCase emits null initially`() = runTest {
        observe().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SyncUserUseCase creates user and observe reflects it`() = runTest {
        val email = "a@example.com"
        val result = sync(email)
        assertTrue(result.isSuccess)
        val user = result.getOrNull()!!
        assertEquals(email, user.email)
        assertEquals(1, repo.syncCallsCount())

        observe().test {
            val emitted = awaitItem()
            assertNotNull(emitted)
            assertEquals(email, emitted!!.email)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SyncUserUseCase propagates failure`() = runTest {
        repo.failSyncWith = IllegalStateException("no network")
        val result = sync("x@example.com")
        assertTrue(result.isFailure)
    }

    @Test
    fun `ClearUserUseCase resets current user`() = runTest {
        sync("a@example.com").getOrThrow()
        clear().getOrThrow()
        observe().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Sync emits new user through observe`() = runTest {
        observe().test {
            assertNull(awaitItem())
            sync("b@example.com").getOrThrow()
            val emitted = awaitItem()
            assertNotNull(emitted)
            assertEquals("b@example.com", emitted!!.email)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

/** helper accessor чтобы не делать syncCalls public-var-with-setter. */
private fun FakeUserRepository.syncCallsCount(): Int = syncCalls
