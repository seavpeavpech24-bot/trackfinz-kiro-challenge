package com.trackfinz.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        val IS_ONBOARDED            = booleanPreferencesKey("is_onboarded")
        val IS_LOGGED_IN            = booleanPreferencesKey("is_logged_in")
        val DARK_MODE               = booleanPreferencesKey("dark_mode")
        val CURRENCY                = stringPreferencesKey("currency")
        val BIOMETRIC_ENABLED       = booleanPreferencesKey("biometric_enabled")
        val NOTIFY_OVER_BUDGET      = booleanPreferencesKey("notify_over_budget")
        val NOTIFY_LARGE_EXPENSE    = booleanPreferencesKey("notify_large_expense")
        val LARGE_EXPENSE_THRESHOLD = doublePreferencesKey("large_expense_threshold")
        // Hero gradient colors stored as ARGB long values
        val HERO_GRADIENT_START     = longPreferencesKey("hero_gradient_start")
        val HERO_GRADIENT_END       = longPreferencesKey("hero_gradient_end")
        // Avatar — absolute file path of the saved cropped image
        val AVATAR_PATH             = stringPreferencesKey("avatar_path")
        // Language code: "en", "kh", "fr", etc.
        val LANGUAGE                = stringPreferencesKey("language")
    }

    val isOnboarded: Flow<Boolean>  = pref(IS_ONBOARDED, false)
    val isLoggedIn: Flow<Boolean>   = pref(IS_LOGGED_IN, false)
    val isDarkMode: Flow<Boolean>   = pref(DARK_MODE, false)
    val currency: Flow<String>      = pref(CURRENCY, "USD")
    val biometricEnabled: Flow<Boolean>    = pref(BIOMETRIC_ENABLED, false)
    val notifyOverBudget: Flow<Boolean>    = pref(NOTIFY_OVER_BUDGET, true)
    val notifyLargeExpense: Flow<Boolean>  = pref(NOTIFY_LARGE_EXPENSE, true)
    val largeExpenseThreshold: Flow<Double> = pref(LARGE_EXPENSE_THRESHOLD, 100.0)
    // Default: Teal500 (#00BCD4) → Emerald500 (#4CAF50)
    val heroGradientStart: Flow<Long> = pref(HERO_GRADIENT_START, 0xFF00BCD4L)
    val heroGradientEnd: Flow<Long>   = pref(HERO_GRADIENT_END,   0xFF4CAF50L)
    val avatarPath: Flow<String>      = pref(AVATAR_PATH, "")
    val language: Flow<String>        = pref(LANGUAGE, "en")

    private fun <T> pref(key: Preferences.Key<T>, default: T): Flow<T> =
        context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[key] ?: default }

    suspend fun setOnboarded(v: Boolean)              = edit { it[IS_ONBOARDED] = v }
    suspend fun setLoggedIn(v: Boolean)               = edit { it[IS_LOGGED_IN] = v }
    suspend fun setDarkMode(v: Boolean)               = edit { it[DARK_MODE] = v }
    suspend fun setCurrency(v: String)                = edit { it[CURRENCY] = v }
    suspend fun setBiometricEnabled(v: Boolean)       = edit { it[BIOMETRIC_ENABLED] = v }
    suspend fun setNotifyOverBudget(v: Boolean)       = edit { it[NOTIFY_OVER_BUDGET] = v }
    suspend fun setNotifyLargeExpense(v: Boolean)     = edit { it[NOTIFY_LARGE_EXPENSE] = v }
    suspend fun setLargeExpenseThreshold(v: Double)   = edit { it[LARGE_EXPENSE_THRESHOLD] = v }
    suspend fun setHeroGradientStart(v: Long)         = edit { it[HERO_GRADIENT_START] = v }
    suspend fun setHeroGradientEnd(v: Long)           = edit { it[HERO_GRADIENT_END] = v }
    suspend fun setAvatarPath(v: String)              = edit { it[AVATAR_PATH] = v }
    suspend fun setLanguage(v: String)                = edit { it[LANGUAGE] = v }

    private suspend fun edit(block: (MutablePreferences) -> Unit) =
        context.dataStore.edit(block)
}
