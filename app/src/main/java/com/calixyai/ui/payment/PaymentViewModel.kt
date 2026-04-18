package com.calixyai.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calixyai.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {
    private val _state = MutableStateFlow(PaymentState())
    val state: StateFlow<PaymentState> = _state.asStateFlow()

    fun onIntent(intent: PaymentIntent) {
        when (intent) {
            PaymentIntent.StartTrial -> viewModelScope.launch {
                repository.setPaymentShown()
                _state.value = PaymentState(navigateHome = true)
            }
        }
    }
}
