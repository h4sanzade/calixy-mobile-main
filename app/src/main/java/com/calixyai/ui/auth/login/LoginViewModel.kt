package com.calixyai.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Contract ──────────────────────────────────────────────────────────────────

sealed interface LoginIntent {
    data class Submit(val email: String, val password: String) : LoginIntent
    data object SignInWithGoogle : LoginIntent
}

data class LoginState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateToHome: Boolean = false,
    val navigateToVerify: Boolean = false
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class LoginViewModel @Inject constructor(
    // Inject your AuthRepository here when Firebase / backend is set up:
    // private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.Submit -> login(intent.email, intent.password)
            LoginIntent.SignInWithGoogle -> signInWithGoogle()
        }
    }

    private fun login(email: String, password: String) {
        if (!validateInputs(email, password)) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // TODO: Replace with real auth call:
            // val result = authRepository.login(email, password)
            delay(1200) // simulated network call

            // Simulated success — swap with real result handling:
            _state.value = _state.value.copy(isLoading = false, navigateToHome = true)

            // On email-not-verified error:
            // _state.value = _state.value.copy(isLoading = false, navigateToVerify = true)

            // On bad credentials:
            // _state.value = _state.value.copy(isLoading = false, error = "Incorrect email or password.")
        }
    }

    private fun signInWithGoogle() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            // TODO: trigger Google Sign-In flow via Activity result launcher
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                _state.value = _state.value.copy(error = "Please enter your email address.")
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _state.value = _state.value.copy(error = "Please enter a valid email address.")
                false
            }
            password.isBlank() -> {
                _state.value = _state.value.copy(error = "Please enter your password.")
                false
            }
            else -> true
        }
    }
}
