package com.calixyai.ui.chatsetup

import com.calixyai.domain.model.ChatMessage
import com.calixyai.domain.model.ChatStep
import com.calixyai.domain.model.SetupProfile

sealed interface ChatSetupIntent {
    data object Initialize : ChatSetupIntent
    data class SubmitText(val value: String) : ChatSetupIntent
    data class SelectOption(val value: String) : ChatSetupIntent
    data class ToggleMultiSelect(val value: String) : ChatSetupIntent
    data class SubmitMultiSelect(val customValue: String? = null) : ChatSetupIntent
}

data class ChartPoint(val month: Float, val weight: Float)

data class BmiUi(
    val bmi: Float,
    val verdict: String,
    val estimatedMonths: Int,
    val progress: Int
)

data class FinalAnalysisUi(
    val currentBmi: Float,
    val targetBmi: Float,
    val estimatedDuration: Int,
    val dailyCalories: Int,
    val chartPoints: List<ChartPoint>
)

data class ChatSetupState(
    val isLoading: Boolean = false,
    val step: ChatStep = ChatStep.FIRST_NAME,
    val messages: List<ChatMessage> = emptyList(),
    val chips: List<String> = emptyList(),
    val multiSelect: Boolean = false,
    val showInput: Boolean = true,
    val selectedItems: Set<String> = emptySet(),
    val profile: SetupProfile = SetupProfile(),
    val bmiUi: BmiUi? = null,
    val finalAnalysisUi: FinalAnalysisUi? = null,
    val finished: Boolean = false
)
