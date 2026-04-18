package com.calixyai.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calixyai.R
import com.calixyai.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val pages = listOf(
        OnboardingPageUi("Your body. Your data. Your rules.", "CalixyAI turns your daily habits into a clear, personalized nutrition rhythm.", R.raw.onboarding_fitness_1),
        OnboardingPageUi("Smart tracking without the noise.", "Log progress fast, understand calories and macros, and stay focused on what matters.", R.raw.onboarding_fitness_2),
        OnboardingPageUi("A premium coach for real life.", "Context-aware plans, habit reminders, and beautiful insights that keep you moving.", R.raw.onboarding_fitness_3)
    )

    private val _state = MutableStateFlow(OnboardingState(pages = pages))
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            OnboardingIntent.Skip -> finish()
            OnboardingIntent.Next -> {
                val current = _state.value.currentPage
                if (current == pages.lastIndex) finish() else _state.value = _state.value.copy(currentPage = current + 1)
            }
            is OnboardingIntent.PageChanged -> _state.value = _state.value.copy(currentPage = intent.page)
        }
    }

    private fun finish() = viewModelScope.launch {
        repository.setOnboardingDone()
        _state.value = _state.value.copy(finished = true)
    }
}
