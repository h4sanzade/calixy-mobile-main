package com.calixyai.ui.auth.verify

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

sealed interface EmailVerifyIntent {
    data class VerifyCode(val email: String, val code: String) : EmailVerifyIntent
    data class ResendCode(val email: String) : EmailVerifyIntent
    data object CooldownFinished : EmailVerifyIntent
}

// ── State ─────────────────────────────────────────────────────────────────────

data class EmailVerifyState(
    val isVerifying: Boolean = false,
    val isSending: Boolean = false,
    val isCooldown: Boolean = false,
    val cooldownSeconds: Int = 60,
    val statusMessage: String? = null,
    val error: String? = null,
    val navigateToHome: Boolean = false
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class EmailVerifyViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EmailVerifyState())
    val state: StateFlow<EmailVerifyState> = _state.asStateFlow()

    fun onIntent(intent: EmailVerifyIntent) {
        when (intent) {
            is EmailVerifyIntent.VerifyCode -> verifyCode(intent.email, intent.code)
            is EmailVerifyIntent.ResendCode -> resendCode(intent.email)
            EmailVerifyIntent.CooldownFinished -> {
                _state.value = _state.value.copy(isCooldown = false, statusMessage = null)
            }
        }
    }

    private fun verifyCode(email: String, code: String) {
        if (code.isBlank() || code.length < 6) {
            _state.value = _state.value.copy(error = "Please enter the 6-digit code.")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isVerifying = true,
                error = null,
                statusMessage = null
            )

            when (val result = authRepository.verifyEmail(email, code)) {
                is NetworkResult.Success -> {
                    // Tokens already saved in repository
                    _state.value = _state.value.copy(
                        isVerifying = false,
                        navigateToHome = true
                    )
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(
                        isVerifying = false,
                        error = result.message
                    )
                }
            }
        }
    }

    private fun resendCode(email: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isSending = true,
                statusMessage = null,
                error = null
            )

            when (val result = authRepository.resendVerification(email)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        isSending = false,
                        isCooldown = true,
                        cooldownSeconds = 60,
                        statusMessage = result.data.message
                    )
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(
                        isSending = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun clearNavigation() {
        _state.value = _state.value.copy(navigateToHome = false)
    }
}