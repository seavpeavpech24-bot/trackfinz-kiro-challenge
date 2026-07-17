package com.trackfinz.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackfinz.app.data.datastore.UserPreferences
import com.trackfinz.app.data.repository.UserRepository
import com.trackfinz.app.i18n.AppLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val prefs: UserPreferences,
    private val userRepo: UserRepository
) : ViewModel() {

    val isDarkMode             = prefs.isDarkMode.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val currency               = prefs.currency.stateIn(viewModelScope, SharingStarted.Eagerly, "USD")
    val user                   = userRepo.userFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val notifyOverBudget       = prefs.notifyOverBudget.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val notifyLargeExpense     = prefs.notifyLargeExpense.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val largeExpenseThreshold  = prefs.largeExpenseThreshold.stateIn(viewModelScope, SharingStarted.Eagerly, 100.0)
    val biometricEnabled       = prefs.biometricEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val heroGradientStart      = prefs.heroGradientStart.stateIn(viewModelScope, SharingStarted.Eagerly, 0xFF00BCD4L)
    val heroGradientEnd        = prefs.heroGradientEnd.stateIn(viewModelScope, SharingStarted.Eagerly, 0xFF4CAF50L)
    val avatarPath             = prefs.avatarPath.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val language               = prefs.language.stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    fun toggleDarkMode()                   = viewModelScope.launch { prefs.setDarkMode(!isDarkMode.value) }
    fun setCurrency(code: String)          = viewModelScope.launch { prefs.setCurrency(code) }
    fun setNotifyOverBudget(v: Boolean)    = viewModelScope.launch { prefs.setNotifyOverBudget(v) }
    fun setNotifyLargeExpense(v: Boolean)  = viewModelScope.launch { prefs.setNotifyLargeExpense(v) }
    fun setLargeExpenseThreshold(v: Double)= viewModelScope.launch { prefs.setLargeExpenseThreshold(v) }
    fun setBiometric(v: Boolean)           = viewModelScope.launch { prefs.setBiometricEnabled(v) }
    fun setHeroGradientStart(v: Long)      = viewModelScope.launch { prefs.setHeroGradientStart(v) }
    fun setHeroGradientEnd(v: Long)        = viewModelScope.launch { prefs.setHeroGradientEnd(v) }
    fun setAvatarPath(v: String)           = viewModelScope.launch { prefs.setAvatarPath(v) }
    fun setLanguage(code: String)          = viewModelScope.launch { prefs.setLanguage(code) }

    /** Returns true if old PIN matches, then updates to new PIN */
    suspend fun changePin(oldPin: String, newPin: String): Boolean {
        val ok = userRepo.verifyPin(oldPin)
        if (ok) {
            val u = userRepo.getUser() ?: return false
            userRepo.update(u.copy(pin = newPin))
        }
        return ok
    }

    fun updateProfile(name: String, email: String) = viewModelScope.launch {
        val u = userRepo.getUser() ?: return@launch
        userRepo.update(u.copy(name = name, email = email))
    }
}
