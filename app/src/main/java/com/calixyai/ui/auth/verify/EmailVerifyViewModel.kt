package com.calixyai.ui.auth.verify

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

sealed interface EmailVerifyIntent {
    data class ResendEmail(val email: String) : EmailVerifyIntent
    data object CooldownFinished : EmailVerifyIntent
}

data class EmailVerifyState(
    val isSending: Boolean = false,
    val isCooldown: Boolean = false,
    val cooldownSeconds: Int = 60,
    val statusMessage: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class EmailVerifyViewModel @Inject constructor(
    // private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EmailVerifyState())
    val state: StateFlow<EmailVerifyState> = _state.asStateFlow()

    fun onIntent(intent: EmailVerifyIntent) {
        when (intent) {
            is EmailVerifyIntent.ResendEmail -> resendEmail(intent.email)
            EmailVerifyIntent.CooldownFinished -> {
                _state.value = _state.value.copy(isCooldown = false, statusMessage = null)
            }
        }
    }

    private fun resendEmail(email: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSending = true, statusMessage = null)

            // TODO: Replace with real call:
            // authRepository.resendVerificationEmail(email)
            delay(1000)

            _state.value = _state.value.copy(
                isSending = false,
                isCooldown = true,
                cooldownSeconds = 60,
                statusMessage = "Verification email sent!"
            )
        }
    }
}
