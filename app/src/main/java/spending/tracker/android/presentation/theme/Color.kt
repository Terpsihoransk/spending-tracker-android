package spending.tracker.android.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Палитра Spending Tracker. Обновлена: тёплые, мягкие оттенки.
 * Фон — тёмно-синий сланец, карточки — светлее, акцент — приглушённый фиолетовый.
 *
 * Приложение только в тёмной теме (Dark-first), без Dynamic Color.
 */

// --- Фон / поверхности ---
val BackgroundDark = Color(0xFF1E2130)      // тёплый тёмно-синий сланец
val SurfaceDark = Color(0xFF2A2F45)         // карточки, чуть светлее фона
val SurfaceVariantDark = Color(0xFF363D56)  // средний между фоном и карточками
val NavigationBarBackground = Color(0xFF181B28) // нижняя навигация, темнее фона

// --- Акцентные ---
val Primary = Color(0xFF9B8AC4)             // тёплый приглушённый фиолетовый
val PrimaryContainer = Color(0xFFB8A8D8)    // светлый фиолетовый для контейнеров
val Secondary = Color(0xFF7B9AC4)           // приглушённый сине-фиолетовый
val Tertiary = Color(0xFFB87AAC)            // пыльно-розовый

// --- Текст / контент ---
val OnSurface = Color(0xFFE8E4F0)            // основной — тёплый почти белый
val OnSurfaceVariant = Color(0xFF9890B4)    // второстепенный — приглушённый серо-фиолетовый
val OnSurfaceDim = Color(0xFF686080)        // подсказки — тёмный приглушённый
val OnSurfaceDisabled = Color(0xFF484060)   // disabled — очень тёмный

// --- Outline / разделители ---
val OutlineDark = Color(0xFF3D3A52)        // средний акцент
val OutlineVariantDark = Color(0xFF4A4766) // светлее для hover states

// --- Семантические (для StatCard и т.п.) ---
val Positive = Color(0xFF10B981)            // зелёный (без изменений)
val Negative = Color(0xFFEF4444)            // красный (без изменений)
val Warning = Color(0xFFF59E0B)             // оранжевый (без изменений)

// --- Цвета категорий для PieChart / StatCard ---
// Приглушённые, гармоничные оттенки
val CategoryColors: List<Color> = listOf(
    Color(0xFF9B8AC4),  // тёплый фиолетовый
    Color(0xFF6BB8A0),  // приглушённый мятно-зелёный
    Color(0xFFC4986A),  // тёплый оранжевый (бежево-коричневый)
    Color(0xFFC47A9B),  // пыльно-розовый
    Color(0xFF7A9BC4),  // приглушённый синий
    Color(0xFF98B86A),  // приглушённый зелёный
    Color(0xFFC4B86A),  // тёплый золотистый
    Color(0xFF6AB8C4),  // мягкий бирюзовый
)
