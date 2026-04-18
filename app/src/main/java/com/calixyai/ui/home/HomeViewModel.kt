package com.calixyai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calixyai.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Load -> observe()
        }
    }

    private fun observe() = viewModelScope.launch {
        repository.observeProfile().collect { profile ->
            if (profile != null) {
                _state.value = HomeState(
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
