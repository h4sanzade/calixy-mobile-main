package com.calixyai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calixyai.data.local.FirstTimeUserStore
import com.calixyai.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AppRepository,
    private val firstTimeUserStore: FirstTimeUserStore
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    // One-shot effects (navigation, etc.) delivered via Channel
    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Load -> observe()
            HomeIntent.CheckFirstTimeUser -> checkFirstTimeUser()
            HomeIntent.ShowAICoachDialog -> _state.update { it.copy(showDialog = true) }
            HomeIntent.ContinueClicked -> onContinueClicked()
        }
    }

    // ── Load profile ──────────────────────────────────────────────────────────

    private fun observe() = viewModelScope.launch {
        repository.observeProfile().collect { profile ->
            if (profile != null) {
                _state.update {
                    it.copy(
                        greeting = "Welcome back",
                        name = "${profile.firstName} ${profile.lastName}",
                        goal = "Goal: Personalized plan active",
                        bmi = "BMI ${profile.bmi}",
                        calories = "Daily rhythm unlocked"
                    )
                }
            }
        }
    }


    private fun checkFirstTimeUser() = viewModelScope.launch {
        val isFirst = firstTimeUserStore.isFirstTimeUser.first()
        _state.update { it.copy(isFirstTimeUser = isFirst) }

        if (isFirst) {
            delay(1_500L)
            onIntent(HomeIntent.ShowAICoachDialog)
        }
    }



    private fun onContinueClicked() = viewModelScope.launch {
        // Dismiss dialog in state
        _state.update { it.copy(showDialog = false, isFirstTimeUser = false) }
        // Persist: never show again
        firstTimeUserStore.markNotFirstTime()
        // Fire navigation effect
        _effects.send(HomeEffect.NavigateToChatbot)
    }
}