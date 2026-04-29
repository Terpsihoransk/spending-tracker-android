package spending.tracker.android.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Скругления согласно мокапу:
 *  - карточки / sheet  : 12dp (small/medium)
 *  - большие поверхности: 16dp (large)
 *  - BottomSheet header : 20dp (extra-large)
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)
