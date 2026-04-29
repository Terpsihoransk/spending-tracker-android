package spending.tracker.android.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Хранит `email` текущего пользователя (MVP1: один пользователь на устройстве).
 *
 * Используется для отображения стартового экрана [`EmailEntryScreen`] и
 * подстановки `userId` в вызовы API/репозиториев.
 */
private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "session_prefs",
)

class SessionManager(private val context: Context) {
    private val emailKey = stringPreferencesKey("current_email")

    val emailFlow: Flow<String> = context.sessionDataStore.data
        .map { prefs -> prefs[emailKey] ?: "" }

    suspend fun setEmail(email: String) {
        context.sessionDataStore.edit { it[emailKey] = email }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.remove(emailKey) }
    }

    /** Алиас для согласованности с API очистки данных пользователя */
    suspend fun clearSession() = clear()
}
