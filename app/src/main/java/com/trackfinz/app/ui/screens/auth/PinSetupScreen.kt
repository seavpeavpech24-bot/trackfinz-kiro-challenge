package com.trackfinz.app.ui.screens.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackfinz.app.ui.components.GradientButton
import com.trackfinz.app.viewmodel.AuthViewModel
import com.trackfinz.app.i18n.LocalStrings
import com.trackfinz.app.i18n.Str

@Composable
fun PinSetupScreen(
    onPinSet: () -> Unit,
    userName: String = "User",
    userEmail: String = "user@trackfinz.com",
    vm: AuthViewModel = hiltViewModel()
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(0) } // 0=enter, 1=confirm
    var error by remember { mutableStateOf<String?>(null) }
    val s = LocalStrings.current

    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.success) { if (state.success) onPinSet() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (step == 0) s(Str.SET_PIN) else s(Str.CONFIRM_PIN),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (step == 0) s(Str.SET_PIN_HINT) else s(Str.CONFIRM_PIN_HINT),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(40.dp))

        PinInput(pin = if (step == 0) pin else confirm, onPinChange = {
            if (step == 0) pin = it else confirm = it
        })

        Spacer(Modifier.height(16.dp))

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))

        GradientButton(
            text = if (step == 0) s(Str.CONTINUE) else s(Str.SET_PIN),
            onClick = {
                if (step == 0) {
                    if (pin.length == 4) { step = 1; error = null }
                    else error = s(Str.PIN_MUST_4)
                } else {
                    if (confirm == pin) {
                        vm.register(userName, userEmail, pin)
                    } else {
                        error = s(Str.PINS_DONT_MATCH)
                        confirm = ""
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = (if (step == 0) pin else confirm).length == 4
        )
    }
}

@Composable
fun PinLockScreen(onUnlocked: () -> Unit, vm: AuthViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var pin by remember { mutableStateOf("") }
    val s = LocalStrings.current
    LaunchedEffect(state.success) { if (state.success) onUnlocked() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔐", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(8.dp))
        Text(s(Str.APP_LOCKED), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(40.dp))
        PinInput(pin = pin, onPinChange = { pin = it })
        Spacer(Modifier.height(32.dp))
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        GradientButton(
            text = s(Str.UNLOCK),
            onClick = { vm.login(pin) },
            modifier = Modifier.fillMaxWidth(),
            enabled = pin.length == 4
        )
    }
}

/** Reusable PIN dot display + numpad */
@Composable
fun PinInput(pin: String, onPinChange: (String) -> Unit) {
    // Dot indicators
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(4) { i ->
            val filled = i < pin.length
            val color by animateColorAsState(
                targetValue = if (filled) MaterialTheme.colorScheme.primary else Color.Transparent,
                label = "pin_dot"
            )
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }

    Spacer(Modifier.height(32.dp))

    // Numpad
    val keys = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Spacer(Modifier.size(80.dp, 64.dp))
                    } else {
                        OutlinedButton(
                            onClick = {
                                when (key) {
                                    "⌫" -> if (pin.isNotEmpty()) onPinChange(pin.dropLast(1))
                                    else -> if (pin.length < 4) onPinChange(pin + key)
                                }
                            },
                            modifier = Modifier.size(80.dp, 64.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (key == "⌫") {
                                Icon(Icons.Default.Backspace, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                            } else {
                                Text(key, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
