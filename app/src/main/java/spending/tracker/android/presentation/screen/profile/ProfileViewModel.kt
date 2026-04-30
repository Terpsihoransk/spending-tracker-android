package spending.tracker.android.presentation.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import spending.tracker.android.data.local.prefs.SessionManager
import spending.tracker.android.domain.model.User
import spending.tracker.android.domain.usecase.ClearUserUseCase
import spending.tracker.android.domain.usecase.ObserveCurrentUserUseCase
import spending.tracker.android.domain.usecase.RefreshCategoriesUseCase
import spending.tracker.android.domain.usecase.RefreshSpendingsUseCase
import spending.tracker.android.domain.usecase.SyncUserUseCase

data class ProfileUiState(
    val user: User? = null,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
)

class ProfileViewModel(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val clearUser: ClearUserUseCase,
    private val sessionManager: SessionManager,
    private val syncUser: SyncUserUseCase,
    private val refreshSpendings: RefreshSpendingsUseCase,
    private val refreshCategories: RefreshCategoriesUseCase,
) : ViewModel() {

    private val error = MutableStateFlow<String?>(null)
    private val isRefreshing = MutableStateFlow(false)

    val state: StateFlow<ProfileUiState> = combine(
        observeCurrentUser(),
        error,
        isRefreshing,
    ) { user, err, refreshing ->
        ProfileUiState(user = user, errorMessage = err, isRefreshing = refreshing)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(),
    )

    /** Разлогин: чистим Room-кэш пользователя и DataStore. */
    fun logout() {
        viewModelScope.launch {
            clearUser()
            sessionManager.clear()
        }
    }

    fun clearError() {
        error.value = null
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                val email = sessionManager.emailFlow.firstOrNull() ?: ""
                if (email.isNotEmpty()) {
                    syncUser(email).getOrThrow()
                    refreshSpendings(email).getOrThrow()
                    refreshCategories(email).getOrThrow()
                }
            } catch (e: Exception) {
                error.value = e.message
            } finally {
                isRefreshing.value = false
            }
        }
    }
}
