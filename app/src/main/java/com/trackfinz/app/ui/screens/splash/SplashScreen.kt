package com.trackfinz.app.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trackfinz.app.ui.theme.Emerald500
import com.trackfinz.app.ui.theme.Teal500
import com.trackfinz.app.viewmodel.AuthViewModel
import com.trackfinz.app.i18n.LocalStrings
import com.trackfinz.app.i18n.Str
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: (String) -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val s = LocalStrings.current
    var alpha by remember { mutableFloatStateOf(0f) }
    val animAlpha by animateFloatAsState(
        targetValue = alpha,
        animationSpec = tween(800),
        label = "alpha"
    )
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        alpha = 1f
        delay(1800)
        val dest = vm.resolveStartDestination()
        onFinished(dest)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Teal500, Emerald500))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale).alpha(animAlpha)
        ) {
            Text("💰", fontSize = 72.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "TrackFinz",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                s(Str.SPLASH_TAGLINE),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}
