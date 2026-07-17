package com.trackfinz.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.trackfinz.app.i18n.AppLanguage
import com.trackfinz.app.i18n.LocalLanguage
import com.trackfinz.app.i18n.LocalStrings
import com.trackfinz.app.i18n.translate
import com.trackfinz.app.navigation.AppNavGraph
import com.trackfinz.app.navigation.NavRoutes
import com.trackfinz.app.ui.theme.TrackFinzTheme
import com.trackfinz.app.utils.NotificationHelper
import com.trackfinz.app.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // ── Notification permission (Android 13+) ─────────────────────────────────
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // granted or denied — no extra action needed, channel already created
        }

    // ── Gallery / media permission ────────────────────────────────────────────
    // Callback set by ProfileScreen before launching the picker
    var onGalleryPermissionResult: ((Boolean) -> Unit)? = null

    private val galleryPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            onGalleryPermissionResult?.invoke(granted)
            onGalleryPermissionResult = null
        }

    /** Called from ProfileScreen to ensure gallery permission before opening picker. */
    fun requestGalleryPermission(onResult: (Boolean) -> Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            onResult(true)
        } else {
            onGalleryPermissionResult = onResult
            galleryPermissionLauncher.launch(permission)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channel (idempotent)
        NotificationHelper.createChannel(this)

        // Request POST_NOTIFICATIONS on Android 13+ — ask on first launch
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val isDarkMode by settingsVm.isDarkMode.collectAsStateWithLifecycle()
            val languageCode by settingsVm.language.collectAsStateWithLifecycle()
            val appLanguage = AppLanguage.fromCode(languageCode)

            TrackFinzTheme(darkTheme = isDarkMode) {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalStrings provides { key -> translate(key, appLanguage) },
                    LocalLanguage provides appLanguage
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val navController = rememberNavController()
                        AppNavGraph(
                            navController = navController,
                            startDestination = NavRoutes.SPLASH
                        )
                    }
                }
            }
        }
    }
}
