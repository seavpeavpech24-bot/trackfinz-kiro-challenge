package com.trackfinz.app.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackfinz.app.ui.components.GradientButton
import com.trackfinz.app.viewmodel.AuthViewModel
import com.trackfinz.app.i18n.LocalStrings
import com.trackfinz.app.i18n.Str

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegister: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var pin by remember { mutableStateOf("") }
    val s = LocalStrings.current

    LaunchedEffect(state.success) { if (state.success) onLoginSuccess() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔐", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(8.dp))
        Text(s(Str.WELCOME_BACK), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(s(Str.ENTER_PIN_HINT), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

        Spacer(Modifier.height(48.dp))

        PinInput(pin = pin, onPinChange = { pin = it })

        Spacer(Modifier.height(32.dp))

        if (state.error != null) {
            Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }

        GradientButton(
            text = if (state.isLoading) s(Str.VERIFYING) else s(Str.UNLOCK),
            onClick = { vm.login(pin) },
            modifier = Modifier.fillMaxWidth(),
            enabled = pin.length == 4 && !state.isLoading
        )

        Spacer(Modifier.height(24.dp))

        TextButton(onClick = onRegister) { Text(s(Str.CREATE_ACCOUNT)) }
    }
}
