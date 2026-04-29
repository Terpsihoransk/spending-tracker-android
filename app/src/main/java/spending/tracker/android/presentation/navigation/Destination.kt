package spending.tracker.android.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Экраны нижней навигации. Порядок ↔ порядок вкладок в [BottomBar].
 */
sealed class Destination(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    data object Spendings : Destination(
        route = "spendings",
        title = "Расходы",
        icon = Icons.Outlined.Receipt,
    )

    data object Categories : Destination(
        route = "categories",
        title = "Категории",
        icon = Icons.Outlined.Category,
    )

    data object Summary : Destination(
        route = "summary",
        title = "Сводка",
        icon = Icons.Outlined.PieChart,
    )

    data object Profile : Destination(
        route = "profile",
        title = "Профиль",
        icon = Icons.Outlined.Person,
    )

    data object EmailEntry : Destination(
        route = "email_entry",
        title = "Вход",
        icon = Icons.Outlined.Person,
    )

    companion object {
        /** Основные вкладки для BottomBar (в порядке мокапа). */
        val bottomBarItems: List<Destination> = listOf(Spendings, Categories, Summary, Profile)
    }
}
