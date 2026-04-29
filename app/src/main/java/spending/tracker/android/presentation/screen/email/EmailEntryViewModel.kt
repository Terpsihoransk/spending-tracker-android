package spending.tracker.android.presentation.screen.email

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import spending.tracker.android.data.local.prefs.SessionManager
import spending.tracker.android.domain.usecase.SyncUserUseCase

data class EmailEntryState(
    val email: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
)

/** Экран первого запуска: просим email, синхронизируемся с бэком, сохраняем в DataStore. */
class EmailEntryViewModel(
    private val syncUser: SyncUserUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(EmailEntryState())
    val state: StateFlow<EmailEntryState> = _state.asStateFlow()

    init {
        // Предзаполняем email из сохранённой сессии
        viewModelScope.launch {
            sessionManager.emailFlow.collect { savedEmail ->
                if (savedEmail.isNotBlank() && _state.value.email.isBlank()) {
                    _state.value = _state.value.copy(email = savedEmail)
                }
            }
        }
    }

    fun onEmailChange(value: String) {
        _state.value = _state.value.copy(email = value.trim(), errorMessage = null)
    }

    fun submit(onSuccess: () -> Unit) {
        val email = _state.value.email
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.value = _state.value.copy(errorMessage = "Введите корректный email")
            return
        }
        _state.value = _state.value.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            syncUser(email).fold(
                onSuccess = {
                    sessionManager.setEmail(email)
                    _state.value = _state.value.copy(isSubmitting = false)
                    onSuccess()
                },
                onFailure = { err ->
                    _state.value = _state.value.copy(
                        isSubmitting = false,
                        errorMessage = err.message ?: "Не удалось подключиться к серверу",
                    )
                },
            )
        }
    }
}
