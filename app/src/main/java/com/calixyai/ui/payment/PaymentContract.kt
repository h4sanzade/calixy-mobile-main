package com.calixyai.ui.payment

sealed interface PaymentIntent { data object StartTrial : PaymentIntent }

data class PaymentState(val navigateHome: Boolean = false)
