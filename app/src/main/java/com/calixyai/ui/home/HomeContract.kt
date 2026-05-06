package com.calixyai.ui.home


sealed interface HomeIntent {
    data object Load : HomeIntent
    data object CheckFirstTimeUser : HomeIntent
    data object ShowAICoachDialog : HomeIntent
    data object ContinueClicked : HomeIntent
}


data class HomeState(
    val greeting: String = "",
    val name: String = "",
    val goal: String = "",
    val bmi: String = "",
    val calories: String = "",
    // First-time user
    val isFirstTimeUser: Boolean = false,
    val showDialog: Boolean = false
)


sealed interface HomeEffect {
    data object NavigateToChatbot : HomeEffect
}