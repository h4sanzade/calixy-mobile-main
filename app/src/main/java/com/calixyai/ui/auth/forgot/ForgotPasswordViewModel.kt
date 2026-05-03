package com.calixyai.ui.auth.forgot

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calixyai.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


sealed interface ForgotPasswordIntent {
    data class SendReset(val email: String) : ForgotPasswordIntent
}

data class ForgotPasswordState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val showSuccess: Boolean = false
)


@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    @ApplicationContext private val context: Context
    // private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordState())
    val state: StateFlow<ForgotPasswordState> = _state.asStateFlow()

    fun onIntent(intent: ForgotPasswordIntent) {
        when (intent) {
            is ForgotPasswordIntent.SendReset -> sendReset(intent.email)
        }
    }

    private fun sendReset(email: String) {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.value = _state.value.copy(
                error = context.getString(R.string.error_invalid_email)
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // TODO: Replace with real call:
            // authRepository.sendPasswordReset(email)
            delay(1200)

            // Always show success — don't reveal if email exists (security best practice)
            _state.value = _state.value.copy(isLoading = false, showSuccess = true)
        }
    }
}