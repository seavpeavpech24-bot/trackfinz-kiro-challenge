package com.trackfinz.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BarEntry(val label: String, val value: Float)
data class GroupedBarEntry(val label: String, val income: Float, val expense: Float)

// ── Simple vertical bar chart ─────────────────────────────────────────────────

@Composable
fun BarChart(
    entries: List<BarEntry>,
    barColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    showValues: Boolean = true
) {
    if (entries.isEmpty()) return

    val key = remember(entries) { entries.hashCode() }
    var animKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(key) { animKey++ }

    val progress by animateFloatAsState(
        targetValue = if (animKey > 0) 1f else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "bar"
    )

    val maxVal = entries.maxOf { it.value }.coerceAtLeast(1f)
    val gridColor = barColor.copy(alpha = 0.08f)

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            val totalSlots = entries.size
            val slotW = size.width / totalSlots
            val barW = slotW * 0.55f
            val padX = (slotW - barW) / 2f

            // Horizontal grid lines (4 lines)
            repeat(4) { i ->
                val y = size.height * (1f - (i + 1) / 4f)
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            }

            entries.forEachIndexed { i, entry ->
                val barH = (entry.value / maxVal) * size.height * progress
                val x = i * slotW + padX
                val top = size.height - barH

                // Shadow
                drawRoundRect(
                    color = barColor.copy(alpha = 0.12f),
                    topLeft = Offset(x + 3f, top + 3f),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                // Bar
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, top),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(10f, 10f)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            entries.forEach { entry ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(entry.label, fontSize = 11.sp, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1)
                }
            }
        }
    }
}

// ── Grouped bar chart (income vs expense side by side) ────────────────────────

@Composable
fun GroupedBarChart(
    entries: List<GroupedBarEntry>,
    incomeColor: Color,
    expenseColor: Color,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) return

    val key = remember(entries) { entries.hashCode() }
    var animKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(key) { animKey++ }

    val progress by animateFloatAsState(
        targetValue = if (animKey > 0) 1f else 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "grouped_bar"
    )

    val maxVal = entries.maxOf { maxOf(it.income, it.expense) }.coerceAtLeast(1f)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            val slotW = size.width / entries.size
            val barW = slotW * 0.28f
            val gap = slotW * 0.06f

            repeat(4) { i ->
                val y = size.height * (1f - (i + 1) / 4f)
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            }

            entries.forEachIndexed { i, entry ->
                val slotX = i * slotW + (slotW - barW * 2 - gap) / 2f

                // Income bar
                val incH = (entry.income / maxVal) * size.height * progress
                drawRoundRect(incomeColor, Offset(slotX, size.height - incH),
                    Size(barW, incH), CornerRadius(8f, 8f))

                // Expense bar
                val expH = (entry.expense / maxVal) * size.height * progress
                drawRoundRect(expenseColor, Offset(slotX + barW + gap, size.height - expH),
                    Size(barW, expH), CornerRadius(8f, 8f))
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            entries.forEach { entry ->
                Text(entry.label, fontSize = 10.sp, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f), maxLines = 1)
            }
        }
    }
}

// ── Line / trend chart ────────────────────────────────────────────────────────

data class LineEntry(val label: String, val value: Float)

@Composable
fun LineChart(
    entries: List<LineEntry>,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = lineColor.copy(alpha = 0.12f),
    modifier: Modifier = Modifier
) {
    if (entries.size < 2) return

    val key = remember(entries) { entries.hashCode() }
    var animKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(key) { animKey++ }

    val progress by animateFloatAsState(
        targetValue = if (animKey > 0) 1f else 0f,
        animationSpec = tween(1100, easing = FastOutSlowInEasing),
        label = "line"
    )

    val maxVal = entries.maxOf { it.value }.coerceAtLeast(1f)
    val gridColor = lineColor.copy(alpha = 0.08f)

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            val stepX = size.width / (entries.size - 1).coerceAtLeast(1)
            val pts = entries.mapIndexed { i, e ->
                Offset(i * stepX, size.height - (e.value / maxVal) * size.height)
            }
            val visibleCount = (pts.size * progress).toInt().coerceAtLeast(1)

            // Grid
            repeat(4) { i ->
                val y = size.height * (1f - (i + 1) / 4f)
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            }

            // Fill area
            if (visibleCount >= 2) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(pts[0].x, size.height)
                    pts.take(visibleCount).forEach { lineTo(it.x, it.y) }
                    lineTo(pts[visibleCount - 1].x, size.height)
                    close()
                }
                drawPath(path, fillColor)
            }

            // Line segments
            for (i in 0 until visibleCount - 1) {
                drawLine(lineColor, pts[i], pts[i + 1], strokeWidth = 3f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }

            // Dots
            pts.take(visibleCount).forEach { pt ->
                drawCircle(Color.White, radius = 5f, center = pt)
                drawCircle(lineColor, radius = 3.5f, center = pt)
            }
        }

        Spacer(Modifier.height(6.dp))

        // Show only first, middle, last labels to avoid crowding
        Row(modifier = Modifier.fillMaxWidth()) {
            val step = if (entries.size <= 7) 1 else entries.size / 6
            entries.forEachIndexed { i, entry ->
                if (i % step == 0 || i == entries.lastIndex) {
                    Text(entry.label, fontSize = 9.sp, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.weight(1f), maxLines = 1)
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
