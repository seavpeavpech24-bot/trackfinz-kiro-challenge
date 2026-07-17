package com.trackfinz.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackfinz.app.i18n.AppLanguage
import com.trackfinz.app.i18n.LocalStrings
import com.trackfinz.app.i18n.Str
import com.trackfinz.app.ui.components.GradientButton
import com.trackfinz.app.ui.theme.Emerald500
import com.trackfinz.app.ui.theme.Teal500
import com.trackfinz.app.viewmodel.SettingsViewModel

import androidx.compose.ui.graphics.vector.ImageVector

data class OnboardPage(val icon: ImageVector, val titleKey: String, val subtitleKey: String, val gradient: List<Color>)

private val pages = listOf(
    OnboardPage(Icons.Default.BarChart, Str.ONBOARD1_TITLE, Str.ONBOARD1_SUB, listOf(Teal500, Color(0xFF00838F))),
    OnboardPage(Icons.Default.EmojiEvents, Str.ONBOARD2_TITLE, Str.ONBOARD2_SUB, listOf(Color(0xFF7B1FA2), Color(0xFF4A148C))),
    OnboardPage(Icons.Default.Lightbulb, Str.ONBOARD3_TITLE, Str.ONBOARD3_SUB, listOf(Emerald500, Color(0xFF2E7D32)))
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    settingsVm: SettingsViewModel = hiltViewModel()
) {
    var page by remember { mutableIntStateOf(0) }
    val current = pages[page]
    val s = LocalStrings.current
    val languageCode by settingsVm.language.collectAsStateWithLifecycle()
    var showLangMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(current.gradient))
    ) {
        // ── Top bar: Language picker (left) + Skip (right) ────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language button
            TextButton(onClick = { showLangMenu = true }) {
                Icon(Icons.Default.Language, contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    AppLanguage.fromCode(languageCode).nativeName,
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            // Skip button
            TextButton(onClick = onFinished) {
                Text(s(Str.ONBOARD_SKIP), color = Color.White.copy(alpha = 0.8f))
            }
        }

        AnimatedContent(
            targetState = page,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
            label = "onboard_page"
        ) { idx ->
            val p = pages[idx]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = p.icon,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = Color.White
                )
                Spacer(Modifier.height(32.dp))
                Text(
                    text = s(p.titleKey),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = s(p.subtitleKey),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dots indicator
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (i == page) Color.White else Color.White.copy(alpha = 0.4f))
                            .size(if (i == page) 24.dp else 8.dp, 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            GradientButton(
                text = if (page == pages.lastIndex) s(Str.ONBOARD_GET_STARTED) else s(Str.CONTINUE),
                onClick = { if (page < pages.lastIndex) page++ else onFinished() },
                modifier = Modifier.fillMaxWidth(),
                gradientColors = listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.15f))
            )
        }
    }

    // ── Language selection modal ──────────────────────────────────────────────
    if (showLangMenu) {
        LanguageSelectionModal(
            currentLanguageCode = languageCode,
            onDismiss = { showLangMenu = false },
            onLanguageSelected = { code ->
                settingsVm.setLanguage(code)
                showLangMenu = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelectionModal(
    currentLanguageCode: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val s = LocalStrings.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        s(Str.SELECT_LANGUAGE),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // Scrollable language list
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AppLanguage.entries.size) { index ->
                    val lang = AppLanguage.entries[index]
                    val isSelected = lang.code == currentLanguageCode

                    Surface(
                        onClick = { onLanguageSelected(lang.code) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else 
                            Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    lang.nativeName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    lang.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
