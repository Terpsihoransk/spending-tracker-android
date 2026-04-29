package spending.tracker.android.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import spending.tracker.android.domain.model.Category

/** Одна дольная секция диаграммы. */
data class PieSlice(
    val label: String,
    val value: Double,
    val color: Color,
    val category: Category? = null,
)

/**
 * Donut-Pie диаграмма на Canvas.
 * Компонент сам рассчитывает проценты и рисует секции с учётом [strokeWidth].
 */
@Composable
fun PieChartCanvas(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 40f,
) {
    val total = slices.sumOf { it.value }.takeIf { it > 0.0 } ?: 1.0
    Canvas(modifier = modifier) {
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(
            x = (size.width - diameter) / 2f,
            y = (size.height - diameter) / 2f,
        )
        val arcSize = Size(diameter, diameter)

        var startAngle = -90f
        slices.forEach { slice ->
            val sweep = (slice.value / total * 360.0).toFloat()
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth),
            )
            startAngle += sweep
        }
    }
}

/** Легенда для PieChart. Отображается вертикально списком. */
@Composable
fun PieChartLegend(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
) {
    val total = slices.sumOf { it.value }.takeIf { it > 0.0 } ?: 1.0
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        slices.forEach { slice ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(slice.color, CircleShape),
                )
                Spacer(Modifier.padding(horizontal = 4.dp))
                val pct = (slice.value / total * 100).toInt()
                Text(
                    text = "${slice.label} · $pct%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
