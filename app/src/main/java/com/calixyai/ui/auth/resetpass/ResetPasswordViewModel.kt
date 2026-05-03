package com.calixyai.ui.auth.resetpass

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

sealed interface ResetPasswordIntent {
    data class Submit(
        val email: String,
        val code: String,
        val newPassword: String,
        val confirmPassword: String
    ) : ResetPasswordIntent
}

// ── State ─────────────────────────────────────────────────────────────────────

data class ResetPasswordState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val navigateToLogin: Boolean = false
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ResetPasswordState())
    val state: StateFlow<ResetPasswordState> = _state.asStateFlow()

    fun onIntent(intent: ResetPasswordIntent) {
        when (intent) {
            is ResetPasswordIntent.Submit -> resetPassword(
                intent.email,
                intent.code,
                intent.newPassword,
                intent.confirmPassword
            )
        }
    }

    private fun resetPassword(
        email: String,
        code: String,
        newPassword: String,
        confirmPassword: String
    ) {
        // Minimal UI-only validation
        when {
            code.isBlank() -> {
                _state.value = _state.value.copy(error = "Please enter the reset code.")
                return
            }
            newPassword.isBlank() -> {
                _state.value = _state.value.copy(error = "Please enter a new password.")
                return
            }
            newPassword != confirmPassword -> {
                _state.value = _state.value.copy(error = "Passwords do not match.")
                return
            }
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                successMessage = null
            )

            when (val result = authRepository.resetPassword(email, code, newPassword)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = result.data.message,
                        navigateToLogin = true
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

    fun clearNavigation() {
        _state.value = _state.value.copy(navigateToLogin = false)
    }
}