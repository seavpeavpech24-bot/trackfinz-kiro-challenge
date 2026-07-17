package com.trackfinz.app.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trackfinz.app.ui.components.FinanceTextField
import com.trackfinz.app.ui.components.GradientButton
import com.trackfinz.app.i18n.LocalStrings
import com.trackfinz.app.i18n.Str

/**
 * Collects name + email only. Does NOT call register() yet.
 * Passes them forward to PinSetupScreen so registration happens
 * once — with the real PIN.
 */
@Composable
fun RegisterScreen(
    onRegistered: (name: String, email: String) -> Unit,
    onLogin: () -> Unit
) {
    var name  by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val s = LocalStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))

        Icon(Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(8.dp))
        Text(s(Str.CREATE_ACCOUNT), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(s(Str.START_JOURNEY), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

        Spacer(Modifier.height(40.dp))

        FinanceTextField(
            value = name,
            onValueChange = { name = it; error = null },
            label = s(Str.FULL_NAME),
            leadingIcon = Icons.Default.Person,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        FinanceTextField(
            value = email,
            onValueChange = { email = it; error = null },
            label = s(Str.EMAIL_ADDRESS),
            leadingIcon = Icons.Default.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(32.dp))

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }

        GradientButton(
            text = s(Str.CONTINUE),
            onClick = {
                when {
                    name.isBlank()  -> error = s(Str.NAME_REQUIRED)
                    email.isBlank() -> error = s(Str.EMAIL_REQUIRED)
                    else            -> onRegistered(name.trim(), email.trim())
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s(Str.ALREADY_HAVE_ACCOUNT), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onLogin) { Text(s(Str.SIGN_IN)) }
        }
    }
}
