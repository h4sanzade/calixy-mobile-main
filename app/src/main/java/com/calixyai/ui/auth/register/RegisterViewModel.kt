package com.calixyai.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calixyai.data.remote.NetworkResult
import com.calixyai.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Intent ────────────────────────────────────────────────────────────────────

sealed interface RegisterIntent {
    data class Submit(
        val email: String,
        val password: String,
        val confirmPassword: String
    ) : RegisterIntent
    data class GoogleSignUp(val idToken: String) : RegisterIntent
}

// ── State ─────────────────────────────────────────────────────────────────────

enum class RegisterErrorField { EMAIL, PASSWORD, CONFIRM }

data class RegisterState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val errorField: RegisterErrorField? = null,
    val navigateToVerify: Boolean = false,
    val navigateToHome: Boolean = false
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    fun onIntent(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.Submit ->
                register(intent.email, intent.password, intent.confirmPassword)
            is RegisterIntent.GoogleSignUp ->
                googleSignUp(intent.idToken)
        }
    }

    private fun register(email: String, password: String, confirmPassword: String) {
        // Only minimal UI-level validation: empty checks and password match
        val (errorMsg, field) = validateInputs(email, password, confirmPassword)
        if (errorMsg != null) {
            _state.value = _state.value.copy(error = errorMsg, errorField = field)
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                errorField = null
            )

            when (val result = authRepository.register(email, password, confirmPassword)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        navigateToVerify = true
                    )
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    private fun googleSignUp(idToken: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                errorField = null
            )

            when (val result = authRepository.googleLogin(idToken)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        navigateToHome = true
                    )
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    /**
     * Minimal UI-only validation: just blank checks + password match.
     * All business rules (email format, password strength) are owned by the backend.
     */
    private fun validateInputs(
        email: String,
        password: String,
        confirmPassword: String
    ): Pair<String?, RegisterErrorField?> = when {
        email.isBlank() ->
            "Please enter your email." to RegisterErrorField.EMAIL
        password.isBlank() ->
            "Please enter a password." to RegisterErrorField.PASSWORD
        confirmPassword.isBlank() ->
            "Please confirm your password." to RegisterErrorField.CONFIRM
        password != confirmPassword ->
            "Passwords do not match." to RegisterErrorField.CONFIRM
        else -> null to null
    }

    fun clearNavigation() {
        _state.value = _state.value.copy(
            navigateToVerify = false,
            navigateToHome = false
        )
    }
}