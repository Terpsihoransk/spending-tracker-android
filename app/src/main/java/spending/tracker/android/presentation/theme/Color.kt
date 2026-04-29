package spending.tracker.android.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Палитра Spending Tracker. Извлечена из [`plans/Spending Tracker - UI Mockup.html`](plans/Spending%20Tracker%20-%20UI%20Mockup.html).
 *
 * Приложение только в тёмной теме (Dark-first), без Dynamic Color.
 */

// --- Фон / поверхности ---
val BackgroundDark = Color(0xFF0A0A14)       // --bg
val SurfaceDark = Color(0xFF12121F)          // --card
val SurfaceVariantDark = Color(0xFF1A1730)   // --card-light

// --- Акцентные ---
val Primary = Color(0xFF7C3AED)              // --accent (фиолетовый)
val PrimaryContainer = Color(0xFFC084FC)     // --accent-2 (светло-фиолетовый)
val Secondary = Color(0xFF4F46E5)            // --accent-3 (индиго)
val Tertiary = Color(0xFF9333EA)             // --accent-4 (пурпурный)

// --- Текст / контент ---
val OnSurface = Color(0xFFE8E0FF)            // --text
val OnSurfaceVariant = Color(0xFF9080C4)     // --text-muted
val OnSurfaceDim = Color(0xFF5A5278)         // --muted-2
val OnSurfaceDisabled = Color(0xFF4E4670)    // --muted

// --- Outline / разделители ---
val OutlineDark = Color(0xFF27234A)          // --border
val OutlineVariantDark = Color(0xFF3D3268)   // --border-light

// --- Семантические (для StatCard и т.п.) ---
val Positive = Color(0xFF10B981)             // --positive (зелёный)
val Negative = Color(0xFFEF4444)             // --negative (красный)
val Warning = Color(0xFFF59E0B)              // --warning (оранжевый)

// --- Цвета категорий для PieChart / StatCard ---
val CategoryColors: List<Color> = listOf(
    Color(0xFF7C3AED),
    Color(0xFFC084FC),
    Color(0xFF4F46E5),
    Color(0xFF9333EA),
    Color(0xFF10B981),
    Color(0xFFF59E0B),
    Color(0xFFEF4444),
    Color(0xFF06B6D4),
)
