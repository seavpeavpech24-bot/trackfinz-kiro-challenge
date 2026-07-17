package com.trackfinz.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

data class PieSlice(val label: String, val value: Float, val color: Color)

// ── Donut chart (ring style) ──────────────────────────────────────────────────

@Composable
fun DonutChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    centerLabel: String = "",
    centerSubLabel: String = ""
) {
    val key = remember(slices) { slices.hashCode() }
    var animKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(key) { animKey++ }

    val progress by animateFloatAsState(
        targetValue = if (animKey > 0) 1f else 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "donut"
    )

    val total = slices.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
    val gapDeg = 2f  // gap between slices in degrees

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.18f
            val r = (size.minDimension - stroke) / 2f
            val tl = Offset(center.x - r, center.y - r)
            val sz = Size(r * 2, r * 2)
            var start = -90f

            slices.forEach { slice ->
                val sweep = ((slice.value / total) * 360f - gapDeg).coerceAtLeast(0f) * progress
                drawArc(
                    color = slice.color,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = tl,
                    size = sz,
                    style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
                start += (slice.value / total) * 360f * progress
            }
        }

        // Center text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (centerLabel.isNotEmpty()) {
                Text(centerLabel, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, maxLines = 1)
            }
            if (centerSubLabel.isNotEmpty()) {
                Text(centerSubLabel, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1)
            }
        }
    }
}

// ── Full pie chart (filled slices) ────────────────────────────────────────────

@Composable
fun PieChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier
) {
    val key = remember(slices) { slices.hashCode() }
    var animKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(key) { animKey++ }

    val progress by animateFloatAsState(
        targetValue = if (animKey > 0) 1f else 0f,
        animationSpec = tween(1100, easing = FastOutSlowInEasing),
        label = "pie"
    )

    val total = slices.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        var start = -90f
        slices.forEach { slice ->
            val sweep = (slice.value / total) * 360f * progress
            // Filled slice
            drawArc(
                color = slice.color,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = Offset.Zero,
                size = size
            )
            // White separator line
            if (sweep > 1f) {
                val angleRad = Math.toRadians((start + sweep).toDouble())
                drawLine(
                    color = Color.White,
                    start = center,
                    end = Offset(
                        center.x + (size.minDimension / 2f) * cos(angleRad).toFloat(),
                        center.y + (size.minDimension / 2f) * sin(angleRad).toFloat()
                    ),
                    strokeWidth = 2.5f
                )
            }
            start += sweep
        }
    }
}

// ── Legend ────────────────────────────────────────────────────────────────────

@Composable
fun ChartLegend(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    showValues: Boolean = false,
    currency: String = ""
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        slices.forEach { slice ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(slice.color)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = slice.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (showValues) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${String.format("%.0f", slice.value)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = slice.color
                    )
                }
            }
        }
    }
}
