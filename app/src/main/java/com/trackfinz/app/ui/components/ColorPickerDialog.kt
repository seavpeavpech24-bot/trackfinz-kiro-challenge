package com.trackfinz.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Preset palette ────────────────────────────────────────────────────────────
private val PRESET_COLORS = listOf(
    // Teals / Blues
    Color(0xFF00BCD4), Color(0xFF0288D1), Color(0xFF1565C0), Color(0xFF283593),
    Color(0xFF4A148C), Color(0xFF6A1B9A),
    // Greens
    Color(0xFF4CAF50), Color(0xFF2E7D32), Color(0xFF00897B), Color(0xFF00695C),
    // Warm
    Color(0xFFFF6F00), Color(0xFFE65100), Color(0xFFBF360C), Color(0xFFB71C1C),
    Color(0xFFAD1457), Color(0xFF880E4F),
    // Pinks / Purples
    Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7), Color(0xFF3F51B5),
    // Neutrals
    Color(0xFF37474F), Color(0xFF263238), Color(0xFF455A64), Color(0xFF546E7A),
)

/**
 * A dialog that lets the user pick a gradient start and end color for the hero section.
 * Colors are returned as Long (ARGB) values via [onSave].
 */
@Composable
fun HeroGradientPickerDialog(
    currentStart: Long,
    currentEnd: Long,
    onDismiss: () -> Unit,
    onSave: (startArgb: Long, endArgb: Long) -> Unit
) {
    var pickedStart by remember { mutableStateOf(Color(currentStart.toInt())) }
    var pickedEnd   by remember { mutableStateOf(Color(currentEnd.toInt())) }
    // Which slot is being edited: 0 = start, 1 = end
    var editingSlot by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Hero Gradient Colors", fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // ── Live preview ──────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(pickedStart, pickedEnd)))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Preview", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                // ── Slot selectors ────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ColorSlotButton(
                        label = "Start Color",
                        color = pickedStart,
                        selected = editingSlot == 0,
                        modifier = Modifier.weight(1f),
                        onClick = { editingSlot = 0 }
                    )
                    ColorSlotButton(
                        label = "End Color",
                        color = pickedEnd,
                        selected = editingSlot == 1,
                        modifier = Modifier.weight(1f),
                        onClick = { editingSlot = 1 }
                    )
                }

                // ── Preset grid ───────────────────────────────────────────────
                Text(
                    "Pick a color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                ColorGrid(
                    colors = PRESET_COLORS,
                    selectedColor = if (editingSlot == 0) pickedStart else pickedEnd,
                    onColorSelected = { color ->
                        if (editingSlot == 0) pickedStart = color else pickedEnd = color
                    }
                )

                // ── Hex input ─────────────────────────────────────────────────
                HexColorInput(
                    color = if (editingSlot == 0) pickedStart else pickedEnd,
                    onColorChange = { color ->
                        if (editingSlot == 0) pickedStart = color else pickedEnd = color
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    pickedStart.toArgb().toLong(),
                    pickedEnd.toArgb().toLong()
                )
            }) { Text("Apply", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ColorSlotButton(
    label: String,
    color: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val borderWidth = if (selected) 2.dp else 1.dp
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
            )
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}

@Composable
private fun ColorGrid(
    colors: List<Color>,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    val columns = 6
    val rows = (colors.size + columns - 1) / columns
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(columns) { col ->
                    val idx = row * columns + col
                    if (idx < colors.size) {
                        val c = colors[idx]
                        val isSelected = c == selectedColor
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onColorSelected(c) }
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun HexColorInput(color: Color, onColorChange: (Color) -> Unit) {
    var hexText by remember(color) {
        val argb = color.toArgb()
        val hex = String.format("%06X", argb and 0xFFFFFF)
        mutableStateOf(hex)
    }
    var isError by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = hexText,
        onValueChange = { raw ->
            val cleaned = raw.trimStart('#').uppercase().filter { it.isLetterOrDigit() }.take(6)
            hexText = cleaned
            if (cleaned.length == 6) {
                try {
                    val parsed = android.graphics.Color.parseColor("#$cleaned")
                    onColorChange(Color(parsed))
                    isError = false
                } catch (_: Exception) { isError = true }
            } else {
                isError = cleaned.isNotEmpty()
            }
        },
        label = { Text("Hex Color") },
        prefix = { Text("#") },
        isError = isError,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
            )
        }
    )
}
