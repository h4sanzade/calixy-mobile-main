package com.calixyai.ui.auth.register

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

sealed interface RegisterIntent {
    data class Submit(
        val email: String,
        val password: String,
        val confirmPassword: String
    ) : RegisterIntent
    data object SignUpWithGoogle : RegisterIntent
}

enum class RegisterErrorField { EMAIL, PASSWORD, CONFIRM }

data class RegisterState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val errorField: RegisterErrorField? = null,
    val navigateToVerify: Boolean = false
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class RegisterViewModel @Inject constructor(
    // private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    fun onIntent(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.Submit -> register(intent.email, intent.password, intent.confirmPassword)
            RegisterIntent.SignUpWithGoogle -> signUpWithGoogle()
        }
    }

    private fun register(email: String, password: String, confirmPassword: String) {
        val (error, field) = validateInputs(email, password, confirmPassword)
        if (error != null) {
            _state.value = _state.value.copy(error = error, errorField = field)
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, errorField = null)

            // TODO: Replace with real auth call:
            // val result = authRepository.register(email, password)
            delay(1400)

            // Simulated success — send verification email, then navigate:
            _state.value = _state.value.copy(isLoading = false, navigateToVerify = true)
        }
    }

    private fun signUpWithGoogle() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, errorField = null)
            // TODO: trigger Google Sign-In flow
        }
    }

    private fun validateInputs(
        email: String,
        password: String,
        confirmPassword: String
    ): Pair<String?, RegisterErrorField?> = when {
        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
            "Please enter a valid email address." to RegisterErrorField.EMAIL
        password.length < 8 ->
            "Password must be at least 8 characters." to RegisterErrorField.PASSWORD
        !password.any { it.isDigit() } ->
            "Password must contain at least one number." to RegisterErrorField.PASSWORD
        password != confirmPassword ->
            "Passwords do not match." to RegisterErrorField.CONFIRM
        else -> null to null
    }
}