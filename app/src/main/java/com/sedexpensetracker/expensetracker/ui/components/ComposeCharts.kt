package com.sedexpensetracker.expensetracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sedexpensetracker.expensetracker.viewmodel.BarChartDataPoint
import com.sedexpensetracker.expensetracker.viewmodel.DailyChartPoint
import com.sedexpensetracker.expensetracker.viewmodel.PieSliceData

@Composable
fun DonutPieChart(
    slices: List<PieSliceData>,
    centerText: String = "",
    centerSubText: String = "",
    modifier: Modifier = Modifier.size(200.dp)
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(slices) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    if (slices.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "No expenses recorded",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val totalAmount = slices.sumOf { it.amountMinorUnits }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 40.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            var startAngle = -90f

            for (slice in slices) {
                val sweepAngle = if (totalAmount > 0) {
                    (slice.amountMinorUnits.toFloat() / totalAmount.toFloat()) * 360f * animatedProgress.value
                } else 0f

                drawArc(
                    color = Color(slice.categoryColor),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth)
                )

                startAngle += sweepAngle
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (centerText.isNotBlank()) {
                Text(
                    text = centerText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (centerSubText.isNotBlank()) {
                Text(
                    text = centerSubText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LineSpendingChart(
    points: List<DailyChartPoint>,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(180.dp),
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    if (points.isEmpty()) return

    val maxAmount = (points.maxOfOrNull { it.amountMinorUnits } ?: 1L).coerceAtLeast(1L)
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(points) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(durationMillis = 700))
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val width = size.width
            val height = size.height
            val spacing = width / (points.size - 1).coerceAtLeast(1)

            val path = Path()
            val fillPath = Path()

            points.forEachIndexed { index, point ->
                val x = index * spacing
                val normalizedY = (point.amountMinorUnits.toFloat() / maxAmount.toFloat()) * animatedProgress.value
                val y = height - (normalizedY * (height - 30.dp.toPx())) - 15.dp.toPx()

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }

                if (index == points.size - 1) {
                    fillPath.lineTo(x, height)
                    fillPath.close()
                }

                // Draw dots
                drawCircle(
                    color = lineColor,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            // Gradient area fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.35f),
                        lineColor.copy(alpha = 0.0f)
                    )
                )
            )

            // Line path
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Labels row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (point in points) {
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BarChart(
    points: List<BarChartDataPoint>,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(200.dp),
    incomeColor: Color = Color(0xFF4CAF50),
    expenseColor: Color = Color(0xFFF44336)
) {
    if (points.isEmpty()) return

    val maxVal = points.maxOf { maxOf(it.incomeMinorUnits, it.expenseMinorUnits) }.coerceAtLeast(1L)
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(points) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val width = size.width
            val height = size.height
            val groupWidth = width / points.size
            val barWidth = 12.dp.toPx()

            points.forEachIndexed { index, point ->
                val groupLeft = index * groupWidth
                val centerX = groupLeft + (groupWidth / 2)

                // Income Bar
                val incomeHeight = (point.incomeMinorUnits.toFloat() / maxVal.toFloat()) * height * animatedProgress.value
                val incomeX = centerX - barWidth - 2.dp.toPx()
                val incomeY = height - incomeHeight

                drawRect(
                    color = incomeColor,
                    topLeft = Offset(incomeX, incomeY),
                    size = Size(barWidth, incomeHeight)
                )

                // Expense Bar
                val expenseHeight = (point.expenseMinorUnits.toFloat() / maxVal.toFloat()) * height * animatedProgress.value
                val expenseX = centerX + 2.dp.toPx()
                val expenseY = height - expenseHeight

                drawRect(
                    color = expenseColor,
                    topLeft = Offset(expenseX, expenseY),
                    size = Size(barWidth, expenseHeight)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (point in points) {
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
