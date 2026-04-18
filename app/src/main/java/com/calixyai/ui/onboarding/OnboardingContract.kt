package com.calixyai.ui.onboarding

sealed interface OnboardingIntent {
    data object Skip : OnboardingIntent
    data object Next : OnboardingIntent
    data class PageChanged(val page: Int) : OnboardingIntent
}

data class OnboardingPageUi(
    val title: String,
    val subtitle: String,
    val animationRes: Int
)

data class OnboardingState(
    val pages: List<OnboardingPageUi> = emptyList(),
    val currentPage: Int = 0,
    val finished: Boolean = false
)
