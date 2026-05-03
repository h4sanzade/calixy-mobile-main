package com.calixyai.ui.splash

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calixyai.data.local.TokenStore
import com.calixyai.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val repository: AppRepository,
    private val tokenStore: TokenStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SplashState())
    val state: StateFlow<SplashState> = _state.asStateFlow()

    fun onIntent(intent: SplashIntent) {
        when (intent) {
            SplashIntent.Load -> load()
        }
    }

    private fun load() = viewModelScope.launch {
        delay(2000)
        val onboardingState = repository.getOnboardingState()

        val destination = when {
            !onboardingState.isOnboardingDone -> SplashDestination.ONBOARDING

            // User is logged in with valid tokens — go straight to their current step
            tokenStore.hasTokens() -> when {
                !onboardingState.isChatSetupDone -> SplashDestination.CHAT_SETUP
                !onboardingState.isPaymentShown -> SplashDestination.PAYMENT
                else -> SplashDestination.HOME
            }

            // No tokens — send to Login
            else -> SplashDestination.LOGIN
        }

        _state.value = SplashState(isLoading = false, destination = destination)
    }
}