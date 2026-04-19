package com.calixyai.ui.chatsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calixyai.data.repository.AppRepository
import com.calixyai.domain.model.ChatMessage
import com.calixyai.domain.model.ChatStep
import com.calixyai.domain.model.MessageType
import com.calixyai.domain.model.Sender
import com.calixyai.domain.model.SetupProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt

@HiltViewModel
class ChatSetupViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatSetupState())
    val state: StateFlow<ChatSetupState> = _state.asStateFlow()

    fun onIntent(intent: ChatSetupIntent) {
        when (intent) {
            ChatSetupIntent.Initialize -> initialize()
            is ChatSetupIntent.SubmitText -> processText(intent.value)
            is ChatSetupIntent.SelectOption -> processSelection(intent.value)
            is ChatSetupIntent.ToggleMultiSelect -> toggleMulti(intent.value)
            is ChatSetupIntent.SubmitMultiSelect -> submitMulti(intent.customValue)
        }
    }

    private fun initialize() {
        if (_state.value.messages.isNotEmpty()) return
        viewModelScope.launch {                          // ← wrap in coroutine
            enqueueBot("Hey there! I'm Calixy, your personal AI nutrition coach. Before we dive in — what's your first name? 😄")
            _state.value = _state.value.copy(showInput = true, chips = emptyList())
        }
    }

    private fun processText(value: String) = viewModelScope.launch {
        if (value.isBlank()) return@launch
        appendUser(value)
        when (_state.value.step) {
            ChatStep.FIRST_NAME -> {
                val profile = _state.value.profile.copy(firstName = value.trim())
                _state.value = _state.value.copy(profile = profile, step = ChatStep.LAST_NAME)
                enqueueBot("${profile.firstName}! Love it. That's going in the VIP list. 🎉 Now, what's your surname?")
            }
            ChatStep.LAST_NAME -> {
                val profile = _state.value.profile.copy(lastName = value.trim())
                _state.value = _state.value.copy(profile = profile, step = ChatStep.GENDER)
                enqueueBot("${profile.firstName} ${profile.lastName} — sounds like someone who's about to transform their life. Let's keep going! 💪")
                enqueueBot("Alright ${profile.firstName}, quick one — how do you identify? This helps me personalize your plan.")
                _state.value = _state.value.copy(
                    showInput = false,
                    chips = listOf("Male", "Female", "Other"),
                    multiSelect = false
                )
            }
            ChatStep.AGE -> {
                val age = value.filter { it.isDigit() }.toIntOrNull() ?: 0
                val profile = _state.value.profile.copy(age = age)
                _state.value = _state.value.copy(profile = profile, step = ChatStep.HEIGHT_WEIGHT)
                enqueueBot("$age! Prime time. Plenty of good years ahead to build something amazing.")
                enqueueBot("Now for the numbers that actually matter — what's your height (cm) and weight (kg)?")
            }
            ChatStep.HEIGHT_WEIGHT -> {
                val parts = value.split("/", " ", ",").mapNotNull { it.trim().toFloatOrNull() }
                val height = parts.getOrNull(0)?.toInt() ?: 170
                val weight = parts.getOrNull(1) ?: 70f
                val bmi = calculateBmi(height, weight)
                val estimatedMonths = estimateMonths(bmi)
                val verdict = when {
                    bmi < 18.5f -> "Your BMI is %.1f — slightly under the normal zone. We'll focus on stronger nutrition foundations.".format(bmi)
                    bmi < 25f -> "Your BMI is %.1f — right around the healthy range. We'll sharpen consistency and body composition.".format(bmi)
                    bmi < 30f -> "Your BMI is %.1f — you're slightly above normal range. Totally fixable! 🔥 With CalixyAI, people in your range typically reach their ideal weight in 3–4 months.".format(bmi)
                    else -> "Your BMI is %.1f — above the recommended range right now, but we can absolutely improve it step by step.".format(bmi)
                }
                val profile = _state.value.profile.copy(heightCm = height, weightKg = weight, bmi = bmi)
                _state.value = _state.value.copy(
                    profile = profile,
                    step = ChatStep.ACTIVITY,
                    bmiUi = BmiUi(
                        bmi = bmi,
                        verdict = verdict,
                        estimatedMonths = estimatedMonths,
                        progress = (((bmi.coerceIn(15f, 35f) - 15f) / 20f) * 100).roundToInt()
                    )
                )
                enqueueBot(verdict)
                _state.value = _state.value.copy(
                    messages = _state.value.messages + ChatMessage(sender = Sender.BOT, text = "BMI", type = MessageType.BMI_CARD)
                )
                enqueueBot("How would you describe your daily activity? Be honest — Calixy doesn't judge 😂")
                _state.value = _state.value.copy(
                    showInput = false,
                    chips = listOf("🛋️ Couch Potato", "🚶 Light Walker", "🏃 Moderately Active", "🏋️ Gym Regular", "⚡ Athlete Mode", "🧘 Yoga & Zen")
                )
            }
            else -> Unit
        }
    }

    private fun processSelection(value: String) = viewModelScope.launch {
        appendUser(value)
        when (_state.value.step) {
            ChatStep.GENDER -> {
                val profile = _state.value.profile.copy(gender = value)
                _state.value = _state.value.copy(profile = profile, step = ChatStep.AGE, showInput = true, chips = emptyList())
                val response = when (value) {
                    "Male" -> "A man on a mission. Respect. 🫡"
                    "Female" -> "Powerful choice. Let's build something strong together. ✨"
                    else -> "Perfect — individuality matters here. 🌈"
                }
                enqueueBot(response)
                enqueueBot("How old are you? (Don't worry, I won't tell anyone 🤫)")
            }
            ChatStep.ACTIVITY -> {
                val profile = _state.value.profile.copy(activityLevel = value)
                _state.value = _state.value.copy(
                    profile = profile,
                    step = ChatStep.GOAL,
                    chips = listOf("🔥 Lose Weight", "💪 Build Muscle", "📊 Stay at My Weight", "🏃 Improve Fitness", "❤️ Just Be Healthier")
                )
                enqueueBot("Nice. That gives me a much better idea of your rhythm.")
                enqueueBot("What's the mission? Pick your main goal:")
            }
            ChatStep.GOAL -> {
                val profile = _state.value.profile.copy(goal = value)
                _state.value = _state.value.copy(
                    profile = profile,
                    step = ChatStep.ALLERGIES,
                    chips = listOf("🥜 Peanuts", "🥛 Dairy", "🌾 Gluten", "🥚 Eggs", "🐟 Seafood", "🍯 Soy", "🌰 Tree Nuts", "✏️ Custom…"),
                    multiSelect = true,
                    selectedItems = emptySet()
                )
                enqueueBot("Locked in. We're building around $value.")
                enqueueBot("Any foods your body and you are… not on speaking terms with? 🤔")
            }
            else -> Unit
        }
    }

    private fun toggleMulti(value: String) {
        val set = _state.value.selectedItems.toMutableSet()
        if (set.contains(value)) set.remove(value) else set.add(value)
        _state.value = _state.value.copy(selectedItems = set)
    }

    private fun submitMulti(customValue: String?) = viewModelScope.launch {
        val currentSelected = _state.value.selectedItems.toMutableList()
        if (!customValue.isNullOrBlank()) currentSelected.add(customValue)
        appendUser(currentSelected.joinToString(" · ").ifBlank { "None" })
        when (_state.value.step) {
            ChatStep.ALLERGIES -> {
                _state.value = _state.value.copy(
                    profile = _state.value.profile.copy(allergies = currentSelected, customAllergy = customValue),
                    step = ChatStep.DIETARY,
                    chips = listOf("☪️ Halal", "✡️ Kosher", "🌱 Vegan", "🥗 Vegetarian", "🥩 Keto", "🌾 Paleo", "🚫 No Restrictions"),
                    selectedItems = emptySet(),
                    multiSelect = true
                )
                enqueueBot("Helpful. I'll keep those out of your plan.")
                enqueueBot("Last one, I promise! Any dietary rules I should know about?")
            }
            ChatStep.DIETARY -> {
                val profile = _state.value.profile.copy(dietaryRules = currentSelected)
                val finalAnalysis = buildFinalAnalysis(profile)
                _state.value = _state.value.copy(
                    profile = profile,
                    step = ChatStep.COMPLETE,
                    chips = emptyList(),
                    showInput = false,
                    selectedItems = emptySet(),
                    finalAnalysisUi = finalAnalysis
                )
                enqueueBot("Okay ${profile.firstName}, I've crunched the numbers. Let me show you your full picture... 📊")
                _state.value = _state.value.copy(
                    messages = _state.value.messages + ChatMessage(sender = Sender.BOT, text = "ANALYSIS", type = MessageType.ANALYSIS_CARD)
                )
                enqueueBot("Based on everything you've told me, here's your personalized CalixyAI plan. Ready to unlock it? 🚀")
                repository.saveSetup(profile)
                _state.value = _state.value.copy(finished = true)
            }
            else -> Unit
        }
    }

    private suspend fun enqueueBot(text: String) {
        _state.value = _state.value.copy(
            messages = _state.value.messages + ChatMessage(sender = Sender.BOT, text = "typing", type = MessageType.TYPING)
        )
        delay(900)
        _state.value = _state.value.copy(
            messages = _state.value.messages.dropLast(1) + ChatMessage(sender = Sender.BOT, text = text)
        )
    }

    private fun appendUser(text: String) {
        _state.value = _state.value.copy(
            messages = _state.value.messages + ChatMessage(sender = Sender.USER, text = text)
        )
    }

    private fun calculateBmi(heightCm: Int, weightKg: Float): Float {
        val meters = heightCm / 100f
        return (weightKg / meters.pow(2)).let { (it * 10).roundToInt() / 10f }
    }

    private fun estimateMonths(bmi: Float): Int = when {
        bmi < 18.5f -> 3
        bmi < 25f -> 2
        bmi < 30f -> 4
        else -> 6
    }

    private fun buildFinalAnalysis(profile: SetupProfile): FinalAnalysisUi {
        val currentWeight = profile.weightKg ?: 70f
        val targetWeight = when (profile.goal) {
            "🔥 Lose Weight" -> currentWeight - 8f
            "💪 Build Muscle" -> currentWeight + 4f
            else -> currentWeight - 2f
        }
        val months = estimateMonths(profile.bmi ?: 24f)
        val dailyCalories = when (profile.goal) {
            "🔥 Lose Weight" -> 1900
            "💪 Build Muscle" -> 2500
            else -> 2150
        }
        val targetBmi = ((targetWeight / ((profile.heightCm ?: 170) / 100f).pow(2)) * 10).roundToInt() / 10f
        val points = (0..months).map { month ->
            val progress = month / months.toFloat().coerceAtLeast(1f)
            ChartPoint(month.toFloat(), currentWeight + ((targetWeight - currentWeight) * progress))
        }
        return FinalAnalysisUi(
            currentBmi = profile.bmi ?: 0f,
            targetBmi = targetBmi,
            estimatedDuration = months,
            dailyCalories = dailyCalories,
            chartPoints = points
        )
    }
}