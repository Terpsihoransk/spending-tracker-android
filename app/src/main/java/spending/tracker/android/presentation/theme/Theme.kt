package spending.tracker.android.presentation.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Главная тема приложения Spending Tracker.
 *
 * Работает ТОЛЬКО в тёмной версии — мокап [`plans/Spending Tracker - UI Mockup.html`](plans/Spending%20Tracker%20-%20UI%20Mockup.html)
 * нарисован в одной тёмной палитре, и мы сознательно не поддерживаем Light/Dynamic Color.
 */
private val AppColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnSurface,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = BackgroundDark,

    secondary = Secondary,
    onSecondary = OnSurface,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = OnSurface,

    tertiary = Tertiary,
    onTertiary = OnSurface,

    background = BackgroundDark,
    onBackground = OnSurface,

    surface = SurfaceDark,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceVariantDark,
    surfaceContainerHighest = SurfaceVariantDark,
    surfaceContainerLow = BackgroundDark,
    surfaceContainerLowest = BackgroundDark,

    outline = OutlineVariantDark,
    outlineVariant = OutlineDark,

    error = Negative,
    onError = OnSurface,
)

@Composable
fun SpendingTrackerTheme(
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = BackgroundDark.toArgb()
            window.navigationBarColor = BackgroundDark.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
