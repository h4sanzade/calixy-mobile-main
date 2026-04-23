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
    data class ConfirmSliders(val heightCm: Int, val weightKg: Float) : ChatSetupIntent
    /** User tapped the pencil icon on a past message */
    data class EditMessage(val messageId: Long) : ChatSetupIntent
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
    val chartPoints: List<ChartPoint>,
    /** Problem 4: used to label chart start/end points */
    val currentWeight: Float = 0f,
    val targetWeight: Float = 0f
)

data class ChatSetupState(
    val isLoading: Boolean = false,
    val step: ChatStep = ChatStep.FIRST_NAME,
    val messages: List<ChatMessage> = emptyList(),
    val chips: List<String> = emptyList(),
    val multiSelect: Boolean = false,
    val showInput: Boolean = true,
    val showSliders: Boolean = false,
    /** When true: view should request keyboard focus on inputMessage */
    val requestKeyboard: Boolean = false,
    /** When true: custom allergy text field shown & keyboard opens */
    val showCustomInput: Boolean = false,
    val selectedItems: Set<String> = emptySet(),
    val profile: SetupProfile = SetupProfile(),
    val bmiUi: BmiUi? = null,
    val finalAnalysisUi: FinalAnalysisUi? = null,
    val finished: Boolean = false,
    /** Problem 4: inline validation error shown beneath the input field */
    val inputError: String? = null,
    /** Problem 4: hint text for target weight input */
    val inputHint: String? = null
)