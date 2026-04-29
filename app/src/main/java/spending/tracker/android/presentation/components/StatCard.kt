package spending.tracker.android.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Карточка сводной статистики: крупное значение + мелкая подпись.
 * Используется в «Сводке» и опционально на главном экране.
 */
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = accentColor,
            )
        }
    }
}

/** Горизонтальный ряд из двух или трёх [StatCard]. */
@Composable
fun StatRow(
    left: @Composable () -> Unit,
    middle: @Composable () -> Unit = {},
    right: @Composable () -> Unit,
    showThreeColumns: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (showThreeColumns) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) { left() }
            Box(modifier = Modifier.weight(1f)) { middle() }
            Box(modifier = Modifier.weight(1f)) { right() }
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) { left() }
            Box(modifier = Modifier.weight(1f)) { right() }
        }
    }
}
