package com.mojtijek.doktor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

data class ChartDataPoint(val timestamp: Long, val value: Double)

@Composable
fun SimpleLineChart(
    data: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    showLabels: Boolean = true
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Nema podataka za prikaz", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val sortedData = data.sortedBy { it.timestamp }
    val maxValue = sortedData.maxOfOrNull { it.value } ?: 1.0
    val minValue = sortedData.minOfOrNull { it.value } ?: 0.0
    val valueRange = (maxValue - minValue).coerceAtLeast(1.0)

    Column(modifier = modifier) {
        if (showLabels) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Min: %.1f".format(minValue),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Max: %.1f".format(maxValue),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 8.dp)
        ) {
            val width = size.width
            val height = size.height
            val xStep = width / (sortedData.size - 1).coerceAtLeast(1)

            // Draw grid lines
            val gridColor = Color.Gray.copy(alpha = 0.1f)
            for (i in 0..4) {
                val y = height * i / 4f
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw line chart
            val path = Path()
            val points = sortedData.mapIndexed { index, point ->
                val x = index * xStep
                val normalizedValue = (point.value - minValue) / valueRange
                val y = height - (normalizedValue * height).toFloat()
                Offset(x, y)
            }

            if (points.isNotEmpty()) {
                path.moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    path.lineTo(points[i].x, points[i].y)
                }

                // Draw line
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw points
                points.forEach { point ->
                    drawCircle(
                        color = lineColor,
                        radius = 4.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = point
                    )
                }
            }
        }

        if (showLabels && sortedData.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val dateFormat = SimpleDateFormat("d. MMM", Locale("hr", "HR"))
                Text(
                    dateFormat.format(Date(sortedData.first().timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    dateFormat.format(Date(sortedData.last().timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
