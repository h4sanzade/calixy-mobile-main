package com.calixyai.ui.splash

sealed interface SplashIntent { data object Load : SplashIntent }

data class SplashState(
    val isLoading: Boolean = true,
    val destination: SplashDestination? = null
)

enum class SplashDestination { ONBOARDING, LOGIN, CHAT_SETUP, PAYMENT, HOME }