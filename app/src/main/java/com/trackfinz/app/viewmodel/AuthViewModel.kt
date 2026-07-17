package com.trackfinz.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackfinz.app.data.model.UserEntity
import com.trackfinz.app.data.repository.UserRepository
import com.trackfinz.app.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    val isOnboarded = repo.prefs.isOnboarded
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isLoggedIn = repo.prefs.isLoggedIn
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun register(name: String, email: String, pin: String) {
        if (name.isBlank() || email.isBlank() || pin.length != 4) {
            _state.value = AuthUiState(error = "Please fill all fields correctly")
            return
        }
        viewModelScope.launch {
            _state.value = AuthUiState(isLoading = true)
            try {
                repo.register(UserEntity(name = name, email = email, pin = pin))
                _state.value = AuthUiState(success = true)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "register failed", e)
                _state.value = AuthUiState(error = e.message ?: "Registration failed")
            }
        }
    }

    fun login(pin: String) {
        viewModelScope.launch {
            _state.value = AuthUiState(isLoading = true)
            try {
                val ok = repo.verifyPin(pin)
                if (ok) {
                    repo.prefs.setLoggedIn(true)
                    _state.value = AuthUiState(success = true)
                } else {
                    _state.value = AuthUiState(error = "Incorrect PIN")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "login failed", e)
                _state.value = AuthUiState(error = e.message ?: "Login failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try { repo.logout() } catch (e: Exception) {
                Log.e("AuthViewModel", "logout failed", e)
            }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }

    /**
     * Called once from SplashScreen. Reads DataStore directly (not StateFlow),
     * checks DB consistency, repairs if needed, returns the correct route.
     * All exceptions are caught — worst case returns ONBOARDING.
     */
    suspend fun resolveStartDestination(): String {
        return try {
            // Read real persisted values directly from DataStore
            val onboarded = repo.prefs.isOnboarded.first()
            val loggedIn  = repo.prefs.isLoggedIn.first()

            Log.d("AuthViewModel", "resolveStart: onboarded=$onboarded loggedIn=$loggedIn")

            if (!onboarded) return NavRoutes.ONBOARDING

            // DataStore says onboarded — verify user actually exists in DB
            val user = try { repo.getUser() } catch (e: Exception) {
                Log.e("AuthViewModel", "getUser failed", e)
                null
            }

            if (user == null) {
                // DB is empty but DataStore says onboarded — stale state, reset
                Log.w("AuthViewModel", "User not found in DB, resetting flags")
                try {
                    repo.prefs.setOnboarded(false)
                    repo.prefs.setLoggedIn(false)
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Failed to reset prefs", e)
                }
                return NavRoutes.ONBOARDING
            }

            // User exists — check login flag
            if (!loggedIn) return NavRoutes.LOGIN

            NavRoutes.DASHBOARD
        } catch (e: Exception) {
            Log.e("AuthViewModel", "resolveStartDestination failed, defaulting to ONBOARDING", e)
            NavRoutes.ONBOARDING
        }
    }
}
