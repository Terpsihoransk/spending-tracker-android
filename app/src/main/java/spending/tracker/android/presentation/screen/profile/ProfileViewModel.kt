package spending.tracker.android.presentation.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import spending.tracker.android.data.local.prefs.SessionManager
import spending.tracker.android.domain.model.User
import spending.tracker.android.domain.usecase.ClearUserUseCase
import spending.tracker.android.domain.usecase.ObserveCurrentUserUseCase

data class ProfileUiState(
    val user: User? = null,
    val errorMessage: String? = null,
)

class ProfileViewModel(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val clearUser: ClearUserUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<ProfileUiState> = combine(
        observeCurrentUser(),
        error,
    ) { user, err ->
        ProfileUiState(user = user, errorMessage = err)
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
}
