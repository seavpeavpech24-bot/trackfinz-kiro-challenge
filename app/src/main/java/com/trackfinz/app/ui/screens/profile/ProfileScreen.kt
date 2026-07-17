package com.trackfinz.app.ui.screens.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.trackfinz.app.R
import com.trackfinz.app.i18n.AppLanguage
import com.trackfinz.app.i18n.LocalStrings
import com.trackfinz.app.i18n.Str
import com.trackfinz.app.ui.components.AvatarCropDialog
import com.trackfinz.app.ui.components.HeroGradientPickerDialog
import com.trackfinz.app.ui.theme.Emerald500
import com.trackfinz.app.ui.theme.ExpenseRed
import com.trackfinz.app.ui.theme.IncomeGreen
import com.trackfinz.app.ui.theme.Teal500
import com.trackfinz.app.viewmodel.AuthViewModel
import com.trackfinz.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

private enum class ActiveDialog {
    NONE, EDIT_PROFILE, CHANGE_PIN, CURRENCY, LARGE_EXPENSE_THRESHOLD, ABOUT, DONATE, HERO_GRADIENT, SHARE_APP, LANGUAGE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    settingsVm: SettingsViewModel = hiltViewModel(),
    authVm: AuthViewModel = hiltViewModel()
) {
    val user                  by settingsVm.user.collectAsStateWithLifecycle()
    val isDark                by settingsVm.isDarkMode.collectAsStateWithLifecycle()
    val currency              by settingsVm.currency.collectAsStateWithLifecycle()
    val notifyOverBudget      by settingsVm.notifyOverBudget.collectAsStateWithLifecycle()
    val notifyLargeExpense    by settingsVm.notifyLargeExpense.collectAsStateWithLifecycle()
    val largeExpenseThreshold by settingsVm.largeExpenseThreshold.collectAsStateWithLifecycle()
    val biometric             by settingsVm.biometricEnabled.collectAsStateWithLifecycle()
    val heroStartLong         by settingsVm.heroGradientStart.collectAsStateWithLifecycle()
    val heroEndLong           by settingsVm.heroGradientEnd.collectAsStateWithLifecycle()
    val savedAvatarPath       by settingsVm.avatarPath.collectAsStateWithLifecycle()
    val languageCode          by settingsVm.language.collectAsStateWithLifecycle()
    val s                     = LocalStrings.current
    var activeDialog  by remember { mutableStateOf(ActiveDialog.NONE) }
    // URI selected from gallery — triggers crop dialog
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }
    var pinFeedback   by remember { mutableStateOf<String?>(null) }
    val scope         = rememberCoroutineScope()

    val activity = LocalContext.current as? com.trackfinz.app.MainActivity

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { pendingCropUri = it }
    }

    // Wraps gallery launch with a permission check
    fun launchGallery() {
        if (activity != null) {
            activity.requestGalleryPermission { granted ->
                if (granted) imagePicker.launch("image/*")
            }
        } else {
            imagePicker.launch("image/*")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s(Str.PROFILE)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {

            // ── Avatar header ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.horizontalGradient(listOf(Color(heroStartLong.toInt()), Color(heroEndLong.toInt()))))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.10f))),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .border(3.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .clickable { launchGallery() }
                    ) {
                        if (savedAvatarPath.isNotEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(java.io.File(savedAvatarPath))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center) {
                                Text(user?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                                    fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Box(modifier = Modifier.align(Alignment.BottomEnd).size(26.dp).clip(CircleShape)
                            .background(Teal500).border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(user?.name ?: "User", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = Color.White)
                    Text(user?.email ?: "", style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { activeDialog = ActiveDialog.EDIT_PROFILE },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(s(Str.EDIT_PROFILE))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            SettingsSection(s(Str.APPEARANCE)) {
                SettingsToggleRow(if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                    s(Str.DARK_MODE), if (isDark) s(Str.DARK_THEME_ACTIVE) else s(Str.LIGHT_THEME_ACTIVE),
                    isDark) { settingsVm.toggleDarkMode() }
                Divider16()
                SettingsNavRow(Icons.Default.Palette, s(Str.HERO_CARD_COLORS),
                    s(Str.CUSTOMIZE_GRADIENT)) { activeDialog = ActiveDialog.HERO_GRADIENT }
                Divider16()
                val currentLang = AppLanguage.fromCode(languageCode)
                SettingsNavRow(Icons.Default.Language, s(Str.LANGUAGE),
                    currentLang.nativeName) { activeDialog = ActiveDialog.LANGUAGE }
            }

            SettingsSection(s(Str.FINANCE)) {
                SettingsNavRow(Icons.Default.AttachMoney, s(Str.CURRENCY), currency) { activeDialog = ActiveDialog.CURRENCY }
            }

            SettingsSection(s(Str.NOTIFICATIONS)) {
                SettingsToggleRow(Icons.Default.NotificationsActive, s(Str.OVER_BUDGET_ALERT),
                    s(Str.OVER_BUDGET_SUBTITLE), notifyOverBudget) { settingsVm.setNotifyOverBudget(it) }
                Divider16()
                SettingsToggleRow(Icons.Default.Warning, s(Str.LARGE_EXPENSE_ALERT),
                    "${s(Str.LARGE_EXPENSE_SUBTITLE)} ${formatThreshold(largeExpenseThreshold)}",
                    notifyLargeExpense) { settingsVm.setNotifyLargeExpense(it) }
                if (notifyLargeExpense) {
                    Divider16()
                    SettingsNavRow(Icons.Default.TrendingUp, s(Str.EXPENSE_THRESHOLD),
                        formatThreshold(largeExpenseThreshold)) { activeDialog = ActiveDialog.LARGE_EXPENSE_THRESHOLD }
                }
            }

            SettingsSection(s(Str.SECURITY)) {
                SettingsNavRow(Icons.Default.Lock, s(Str.CHANGE_PIN), s(Str.CHANGE_PIN_SUBTITLE)) { activeDialog = ActiveDialog.CHANGE_PIN }
                Divider16()
                SettingsToggleRow(Icons.Default.Fingerprint, s(Str.BIOMETRIC_LOGIN),
                    s(Str.BIOMETRIC_SUBTITLE), biometric) { settingsVm.setBiometric(it) }
            }

            SettingsSection(s(Str.ABOUT)) {
                SettingsInfoRow(Icons.Default.Info, s(Str.VERSION), "1.1.2")
                Divider16()
                SettingsInfoRow(Icons.Default.Person, s(Str.AUTHOR), "SEAVPEAV PECH")
                Divider16()
                SettingsNavRow(Icons.Default.Info, s(Str.APP_INFO)) { activeDialog = ActiveDialog.ABOUT }
                Divider16()
                SettingsNavRow(Icons.Default.Favorite, s(Str.SUPPORT_DEV),
                    s(Str.SUPPORT_DEV_SUBTITLE)) { activeDialog = ActiveDialog.DONATE }
                Divider16()
                SettingsNavRow(Icons.Default.Star, s(Str.RATE_APP)) {}
                Divider16()
                SettingsNavRow(Icons.Default.Share, s(Str.SHARE_APP)) { activeDialog = ActiveDialog.SHARE_APP }
            }

            Spacer(Modifier.height(16.dp))

            pinFeedback?.let { msg ->
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (msg.startsWith("✅")) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)) {
                    Text(msg, modifier = Modifier.padding(12.dp),
                        color = if (msg.startsWith("✅")) IncomeGreen else ExpenseRed,
                        style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = { authVm.logout(); onLogout() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text(s(Str.SIGN_OUT), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Crop dialog — shown when user picks a photo ───────────────────────────
    pendingCropUri?.let { uri ->
        AvatarCropDialog(
            sourceUri = uri,
            onDismiss = { pendingCropUri = null },
            onCropped = { path ->
                settingsVm.setAvatarPath(path)
                pendingCropUri = null
            }
        )
    }

    when (activeDialog) {
        ActiveDialog.EDIT_PROFILE -> EditProfileDialog(user?.name ?: "", user?.email ?: "",
            onDismiss = { activeDialog = ActiveDialog.NONE },
            onSave = { n, e -> settingsVm.updateProfile(n, e); activeDialog = ActiveDialog.NONE })
        ActiveDialog.CHANGE_PIN -> ChangePinDialog(
            onDismiss = { activeDialog = ActiveDialog.NONE; pinFeedback = null },
            onSave = { old, new ->
                scope.launch {
                    val ok = settingsVm.changePin(old, new)
                    pinFeedback = if (ok) s(Str.PIN_UPDATED) else s(Str.PIN_INCORRECT)
                    activeDialog = ActiveDialog.NONE
                }
            })
        ActiveDialog.CURRENCY -> CurrencyDialog(currency,
            onDismiss = { activeDialog = ActiveDialog.NONE },
            onSelect = { settingsVm.setCurrency(it); activeDialog = ActiveDialog.NONE })
        ActiveDialog.LARGE_EXPENSE_THRESHOLD -> ThresholdDialog(largeExpenseThreshold,
            onDismiss = { activeDialog = ActiveDialog.NONE },
            onSave = { settingsVm.setLargeExpenseThreshold(it); activeDialog = ActiveDialog.NONE })
        ActiveDialog.ABOUT -> AboutDialog(onDismiss = { activeDialog = ActiveDialog.NONE })
        ActiveDialog.DONATE -> DonateDialog(onDismiss = { activeDialog = ActiveDialog.NONE })
        ActiveDialog.HERO_GRADIENT -> HeroGradientPickerDialog(
            currentStart = heroStartLong,
            currentEnd   = heroEndLong,
            onDismiss    = { activeDialog = ActiveDialog.NONE },
            onSave       = { start, end ->
                settingsVm.setHeroGradientStart(start)
                settingsVm.setHeroGradientEnd(end)
                activeDialog = ActiveDialog.NONE
            }
        )
        ActiveDialog.SHARE_APP -> ShareAppDialog(onDismiss = { activeDialog = ActiveDialog.NONE })
        ActiveDialog.LANGUAGE -> LanguageDialog(
            currentCode = languageCode,
            onDismiss = { activeDialog = ActiveDialog.NONE },
            onSelect = { code -> settingsVm.setLanguage(code); activeDialog = ActiveDialog.NONE }
        )
        ActiveDialog.NONE -> Unit
    }
}

// ── Layout helpers ────────────────────────────────────────────────────────────

@Composable
private fun Divider16() = HorizontalDivider(
    modifier = Modifier.padding(horizontal = 16.dp),
    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
)

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
        Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
            Column(content = content)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsNavRow(icon: ImageVector, label: String, subtitle: String? = null, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) }
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun SettingsToggleRow(icon: ImageVector, label: String, subtitle: String? = null, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) }
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
private fun SettingsInfoRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

// ── About dialog ──────────────────────────────────────────────────────────────

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.MonetizationOn, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(4.dp))
                Text("TrackFinz", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("v1.1.2 — 100% Free", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AboutInfoBlock(s(Str.AUTHOR), "SEAVPEAV PECH")
                AboutInfoBlock(s(Str.CONTACT_WORK), "seavpeavpech24@gmail.com", clickable = true) {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:seavpeavpech24@gmail.com"))
                    context.startActivity(intent)
                }
                Card(colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null,
                            tint = IncomeGreen, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(s(Str.FREE_FOREVER), fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium, color = IncomeGreen)
                            Text(s(Str.FREE_FOREVER_DESC), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                    }
                }
                Text(s(Str.BUILT_WITH_LOVE), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(s(Str.CLOSE)) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutInfoBlock(label: String, value: String, clickable: Boolean = false, onClick: () -> Unit = {}) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(2.dp))
        if (clickable) {
            Surface(onClick = onClick, shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)) {
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        } else {
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Donate dialog ─────────────────────────────────────────────────────────────

@Composable
private fun DonateDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val s = LocalStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Favorite, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(4.dp))
                Text(s(Str.DONATE_TITLE), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(s(Str.DONATE_SUBTITLE), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), textAlign = TextAlign.Center)
            }
        },
        text = {
            Column {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(s(Str.CAMBODIA)) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(s(Str.INTERNATIONAL)) })
                }
                Spacer(Modifier.height(16.dp))
                if (selectedTab == 0) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(s(Str.SCAN_KHQR), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Text(s(Str.ABA_HINT), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Spacer(Modifier.height(12.dp))
                                AsyncImage(model = ImageRequest.Builder(context).data(R.drawable.khqr).crossfade(true).build(),
                                    contentDescription = "KHQR Code", contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(180.dp).clip(RoundedCornerShape(12.dp)))
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = {
                                    val uri = Uri.parse("https://pay.ababank.com/oRF8/d9hw59ty")
                                    try { context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                                    } catch (_: Exception) { context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); setPackage(null) }) }
                                }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003087))) {
                                    Text(s(Str.PAY_VIA_ABA), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                }
                            }
                        }
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                DonateInfoRow(s(Str.ACCOUNT_NAME), "PECH SEAVPEAV")
                                DonateInfoRow(s(Str.ACCOUNT_NUMBER), "500 263 659", copyable = true, context = context)
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(s(Str.WIRE_SWIFT), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                DonateInfoRow(s(Str.BANK), "Advanced Bank of Asia Ltd (ABA)")
                                DonateInfoRow(s(Str.SWIFT_CODE), "ABAAKHPP", copyable = true, context = context)
                                DonateInfoRow(s(Str.ACCOUNT_NAME), "PECH SEAVPEAV")
                                DonateInfoRow(s(Str.ACCOUNT_NO), "500 263 659", copyable = true, context = context)
                                DonateInfoRow(s(Str.BANK_ADDRESS), "No. 148, Preah Sihanouk Blvd,\nPhnom Penh, Cambodia")
                            }
                        }
                        Text(s(Str.DONATE_THANKS), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(s(Str.CLOSE)) } }
    )
}

@Composable
private fun DonateInfoRow(label: String, value: String, copyable: Boolean = false, context: Context? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.weight(0.42f))
        Row(modifier = Modifier.weight(0.58f), verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f))
            if (copyable && context != null) {
                IconButton(onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText(label, value))
                }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// ── Edit Profile ──────────────────────────────────────────────────────────────

@Composable
private fun EditProfileDialog(currentName: String, currentEmail: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name  by remember { mutableStateOf(currentName) }
    var email by remember { mutableStateOf(currentEmail) }
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s(Str.EDIT_PROFILE), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(s(Str.FULL_NAME)) },
                    leadingIcon = { Icon(Icons.Default.Person, null) }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(s(Str.EMAIL_ADDRESS)) },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim(), email.trim()) }) { Text(s(Str.SAVE)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s(Str.CANCEL)) } }
    )
}

// ── Change PIN ────────────────────────────────────────────────────────────────

@Composable
private fun ChangePinDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var currentPin  by remember { mutableStateOf("") }
    var newPin      by remember { mutableStateOf("") }
    var confirmPin  by remember { mutableStateOf("") }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew     by remember { mutableStateOf(false) }
    var error       by remember { mutableStateOf<String?>(null) }
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s(Str.CHANGE_PIN), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = currentPin, onValueChange = { if (it.length <= 4) currentPin = it },
                    label = { Text(s(Str.CURRENT_PIN_LABEL)) }, leadingIcon = { Icon(Icons.Default.Lock, null) },
                    trailingIcon = { IconButton(onClick = { showCurrent = !showCurrent }) {
                        Icon(if (showCurrent) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } },
                    visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = newPin, onValueChange = { if (it.length <= 4) newPin = it },
                    label = { Text(s(Str.NEW_PIN_4)) }, leadingIcon = { Icon(Icons.Default.LockOpen, null) },
                    trailingIcon = { IconButton(onClick = { showNew = !showNew }) {
                        Icon(if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } },
                    visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = confirmPin, onValueChange = { if (it.length <= 4) confirmPin = it },
                    label = { Text(s(Str.CONFIRM_NEW_PIN)) }, leadingIcon = { Icon(Icons.Default.LockOpen, null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = confirmPin.isNotEmpty() && confirmPin != newPin,
                    supportingText = if (confirmPin.isNotEmpty() && confirmPin != newPin) { { Text(s(Str.PINS_DONT_MATCH)) } } else null,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    currentPin.length != 4 -> error = s(Str.ENTER_CURRENT_PIN_ERR)
                    newPin.length != 4     -> error = s(Str.NEW_PIN_4_ERR)
                    newPin != confirmPin   -> error = s(Str.NEW_PINS_DONT_MATCH)
                    else -> onSave(currentPin, newPin)
                }
            }) { Text(s(Str.UPDATE)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s(Str.CANCEL)) } }
    )
}

// ── Threshold ─────────────────────────────────────────────────────────────────

@Composable
private fun ThresholdDialog(current: Double, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var value by remember { mutableStateOf(current.toInt().toString()) }
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s(Str.THRESHOLD_TITLE), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s(Str.THRESHOLD_HINT), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text(s(Str.AMOUNT)) },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = { TextButton(onClick = { value.toDoubleOrNull()?.let { onSave(it) } }) { Text(s(Str.SAVE)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s(Str.CANCEL)) } }
    )
}

// ── Currency dialog — world currencies ───────────────────────────────────────

private val ALL_CURRENCIES = listOf(
    "KHR" to "🇰🇭 Cambodian Riel",       "USD" to "🇺🇸 US Dollar",
    "EUR" to "🇪🇺 Euro",                  "GBP" to "🇬🇧 British Pound",
    "JPY" to "🇯🇵 Japanese Yen",          "CNY" to "🇨🇳 Chinese Yuan",
    "KRW" to "🇰🇷 South Korean Won",      "THB" to "🇹🇭 Thai Baht",
    "VND" to "🇻🇳 Vietnamese Dong",       "MYR" to "🇲🇾 Malaysian Ringgit",
    "SGD" to "🇸🇬 Singapore Dollar",      "IDR" to "🇮🇩 Indonesian Rupiah",
    "PHP" to "🇵🇭 Philippine Peso",       "MMK" to "🇲🇲 Myanmar Kyat",
    "LAK" to "🇱🇦 Lao Kip",              "BND" to "🇧🇳 Brunei Dollar",
    "INR" to "🇮🇳 Indian Rupee",          "PKR" to "🇵🇰 Pakistani Rupee",
    "BDT" to "🇧🇩 Bangladeshi Taka",      "LKR" to "🇱🇰 Sri Lankan Rupee",
    "NPR" to "🇳🇵 Nepalese Rupee",        "AUD" to "🇦🇺 Australian Dollar",
    "NZD" to "🇳🇿 New Zealand Dollar",    "HKD" to "🇭🇰 Hong Kong Dollar",
    "TWD" to "🇹🇼 Taiwan Dollar",         "MNT" to "🇲🇳 Mongolian Tögrög",
    "CAD" to "🇨🇦 Canadian Dollar",       "MXN" to "🇲🇽 Mexican Peso",
    "BRL" to "🇧🇷 Brazilian Real",        "ARS" to "🇦🇷 Argentine Peso",
    "CLP" to "🇨🇱 Chilean Peso",          "COP" to "🇨🇴 Colombian Peso",
    "PEN" to "🇵🇪 Peruvian Sol",          "CHF" to "🇨🇭 Swiss Franc",
    "SEK" to "🇸🇪 Swedish Krona",         "NOK" to "🇳🇴 Norwegian Krone",
    "DKK" to "🇩🇰 Danish Krone",          "PLN" to "🇵🇱 Polish Zloty",
    "CZK" to "🇨🇿 Czech Koruna",          "HUF" to "🇭🇺 Hungarian Forint",
    "RON" to "🇷🇴 Romanian Leu",          "RUB" to "🇷🇺 Russian Ruble",
    "UAH" to "🇺🇦 Ukrainian Hryvnia",     "TRY" to "🇹🇷 Turkish Lira",
    "SAR" to "🇸🇦 Saudi Riyal",           "AED" to "🇦🇪 UAE Dirham",
    "QAR" to "🇶🇦 Qatari Riyal",          "KWD" to "🇰🇼 Kuwaiti Dinar",
    "BHD" to "🇧🇭 Bahraini Dinar",        "ILS" to "🇮🇱 Israeli Shekel",
    "EGP" to "🇪🇬 Egyptian Pound",        "ZAR" to "🇿🇦 South African Rand",
    "NGN" to "🇳🇬 Nigerian Naira",        "KES" to "🇰🇪 Kenyan Shilling",
    "GHS" to "🇬🇭 Ghanaian Cedi",         "MAD" to "🇲🇦 Moroccan Dirham",
    "TZS" to "🇹🇿 Tanzanian Shilling",    "UGX" to "🇺🇬 Ugandan Shilling"
)

@Composable
private fun CurrencyDialog(current: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    var search by remember { mutableStateOf("") }
    val s = LocalStrings.current
    val filtered = remember(search) {
        if (search.isBlank()) ALL_CURRENCIES
        else ALL_CURRENCIES.filter { (code, name) ->
            code.contains(search, ignoreCase = true) || name.contains(search, ignoreCase = true)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s(Str.SELECT_CURRENCY), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(value = search, onValueChange = { search = it },
                    label = { Text(s(Str.SEARCH_ELLIPSIS)) }, leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(filtered.size) { i ->
                        val (code, name) = filtered[i]
                        Surface(onClick = { onSelect(code) }, modifier = Modifier.fillMaxWidth(),
                            color = if (code == current) Teal500.copy(alpha = 0.1f) else Color.Transparent) {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(name, style = MaterialTheme.typography.bodyMedium)
                                    Text(code, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                if (code == current)
                                    Icon(Icons.Default.Check, null, tint = Teal500, modifier = Modifier.size(18.dp))
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(s(Str.CLOSE)) } }
    )
}

private fun formatThreshold(v: Double): String =
    if (v >= 1000) "$${(v / 1000).toInt()}k" else "$${v.toInt()}"

// ── Share App dialog ──────────────────────────────────────────────────────────

private data class ShareOption(
    val label: String,
    val packageName: String?,
    val action: (Context) -> Unit
)

@Composable
private fun ShareAppDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val driveLink = "https://drive.google.com/drive/folders/1__gFAOTrG2sj8lIQztWV_kvfhvFihcI0?usp=sharing"
    val shareText = "Hey! Check out TrackFinz — a free personal finance tracker.\n" +
            "Track expenses, set budgets & reach your goals. 💰\n" +
            "Download it here: $driveLink"

    val options = remember {
        listOf(
            ShareOption("Facebook", "com.facebook.katana") { ctx ->
                ctx.startShareIntent(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText); setPackage("com.facebook.katana")
                })
            },
            ShareOption("Messenger", "com.facebook.orca") { ctx ->
                ctx.startShareIntent(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText); setPackage("com.facebook.orca")
                })
            },
            ShareOption("WhatsApp", "com.whatsapp") { ctx ->
                ctx.startShareIntent(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText); setPackage("com.whatsapp")
                })
            },
            ShareOption("Telegram", "org.telegram.messenger") { ctx ->
                ctx.startShareIntent(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText); setPackage("org.telegram.messenger")
                })
            },
            ShareOption("Bluetooth", null) { ctx ->
                ctx.startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText)
                    }, "Share via Bluetooth"
                ))
            },
            ShareOption("More", null) { ctx ->
                ctx.startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        putExtra(Intent.EXTRA_SUBJECT, "TrackFinz — Free Finance Tracker")
                    }, "Share TrackFinz"
                ))
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Share, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(4.dp))
                Text(s(Str.SHARE_TITLE), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(s(Str.SHARE_SUBTITLE), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), textAlign = TextAlign.Center)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))) {
                    Row(modifier = Modifier.fillMaxWidth().clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(driveLink)))
                        }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s(Str.DOWNLOAD_DRIVE), style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Text(s(Str.DOWNLOAD_DRIVE_HINT), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                        }
                        Icon(Icons.Default.OpenInNew, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }

                // Share buttons grid — text only
                options.chunked(3).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { option ->
                            ShareOptionButton(
                                label = option.label,
                                modifier = Modifier.weight(1f)
                            ) {
                                option.action(context)
                                onDismiss()
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(s(Str.CANCEL)) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareOptionButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Tries to launch the intent; falls back to the system chooser if the target app isn't installed. */
private fun Context.startShareIntent(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: android.content.ActivityNotFoundException) {
        val fallback = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, intent.getStringExtra(Intent.EXTRA_TEXT))
        }
        startActivity(Intent.createChooser(fallback, "Share TrackFinz"))
    }
}

// ── Language picker dialog ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDialog(
    currentCode: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
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
                    val isSelected = lang.code == currentCode

                    Surface(
                        onClick = { onSelect(lang.code) },
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
