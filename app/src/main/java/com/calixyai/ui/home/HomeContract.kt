package com.calixyai.ui.home

sealed interface HomeIntent { data object Load : HomeIntent }

data class HomeState(
    val greeting: String = "",
    val name: String = "",
    val goal: String = "",
    val bmi: String = "",
    val calories: String = ""
)
