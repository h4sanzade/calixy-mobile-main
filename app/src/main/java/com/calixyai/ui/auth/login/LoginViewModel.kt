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


@HiltViewModel
class LoginViewModel @Inject constructor(
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
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.value = _state.value.copy(error = "Please enter a valid email address.")
            return
        }
        if (password.isBlank()) {
            _state.value = _state.value.copy(error = "Please enter your password.")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // TODO: Replace with real auth call:
            // val result = authRepository.login(email, password)
            delay(1200)

            // Simulated success:
            _state.value = _state.value.copy(isLoading = false, navigateToHome = true)
        }
    }

    private fun signInWithGoogle() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
        }
    }
}