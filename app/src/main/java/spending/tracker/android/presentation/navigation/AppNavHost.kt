package spending.tracker.android.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.koin.compose.koinInject
import spending.tracker.android.data.local.prefs.SessionManager
import spending.tracker.android.presentation.screen.categories.CategoriesScreen
import spending.tracker.android.presentation.screen.email.EmailEntryScreen
import spending.tracker.android.presentation.screen.profile.ProfileScreen
import spending.tracker.android.presentation.screen.spendings.SpendingsScreen
import spending.tracker.android.presentation.screen.summary.SummaryScreen

/**
 * Корневой NavHost. Стартовый экран выбирается на основе наличия email в DataStore:
 *   - если email сохранён → [Destination.Spendings]
 *   - если нет          → [Destination.EmailEntry]
 */
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    val session: SessionManager = koinInject()
    val currentEmail by session.emailFlow.collectAsState(initial = null)

    val startDestination = when (currentEmail) {
        null -> null // ещё не прочитали DataStore — подождём
        "" -> Destination.EmailEntry.route
        else -> Destination.Spendings.route
    } ?: return

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Destination.EmailEntry.route) {
            EmailEntryScreen(
                onLoggedIn = {
                    navController.navigate(Destination.Spendings.route) {
                        popUpTo(Destination.EmailEntry.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Destination.Spendings.route) { SpendingsScreen() }
        composable(Destination.Categories.route) { CategoriesScreen() }
        composable(Destination.Summary.route) { SummaryScreen() }
        composable(Destination.Profile.route) {
            ProfileScreen(
                onLoggedOut = {
                    navController.navigate(Destination.EmailEntry.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
