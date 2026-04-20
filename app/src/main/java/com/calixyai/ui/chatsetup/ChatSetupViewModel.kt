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
            is ChatSetupIntent.ConfirmSliders -> confirmSliders(intent.heightCm, intent.weightKg)
        }
    }

    private fun initialize() {
        if (_state.value.messages.isNotEmpty()) return
        viewModelScope.launch {
            enqueueBot("Hey there! I'm Calixy, your personal AI nutrition coach 🤖\n\nBefore we build your plan — what's your **first name**?")
            _state.value = _state.value.copy(showInput = true, chips = emptyList())
        }
    }

    private fun processText(value: String) = viewModelScope.launch {
        if (value.isBlank()) return@launch
        when (_state.value.step) {
            ChatStep.FIRST_NAME -> {
                val name = value.trim()
                appendUser(name)
                val profile = _state.value.profile.copy(firstName = name)
                _state.value = _state.value.copy(profile = profile, step = ChatStep.LAST_NAME)
                enqueueBot("Love the name **$name**! 🎉 Now, what's your surname?")
            }

            ChatStep.LAST_NAME -> {
                val last = value.trim()
                appendUser(last)
                val profile = _state.value.profile.copy(lastName = last)
                val full = "${profile.firstName} $last"
                _state.value = _state.value.copy(profile = profile, step = ChatStep.GENDER)
                enqueueBot("**$full** — that's a name that means business. Let's keep the momentum going! 💪")
                delay(400)
                enqueueBot("Alright ${profile.firstName}, quick question — how do you identify? This helps me personalize your plan perfectly.")
                _state.value = _state.value.copy(
                    showInput = false,
                    chips = listOf("Male", "Female", "Other"),
                    multiSelect = false
                )
            }

            ChatStep.AGE -> {
                val age = value.filter { it.isDigit() }.toIntOrNull() ?: 0
                appendUser("$age")
                val profile = _state.value.profile.copy(age = age)
                _state.value = _state.value.copy(profile = profile, step = ChatStep.HEIGHT_WEIGHT)

                val ageReply = when {
                    age in 13..17 ->
                        "**$age** years young! 🌟 At your age, building solid habits now will pay off for decades. Your body is still developing, so we'll design a plan that's safe and powerful for you."
                    age in 18..25 ->
                        "**$age** — peak energy years! ⚡ Your metabolism is your superpower right now. Let's use it well."
                    age in 26..35 ->
                        "**$age**! That's the sweet spot where discipline meets experience. 🔥 You know what you want — we'll help you get there faster."
                    age in 36..45 ->
                        "**$age** — and clearly taking health seriously. 💪 Smart move. At this stage, quality nutrition makes an enormous difference in how you feel every single day."
                    age in 46..55 ->
                        "**$age**! Many of our best transformations happen in this decade. 🌿 With the right plan, energy and vitality are absolutely within reach."
                    age in 56..65 ->
                        "**$age** — wisdom and wellness together. 🧘 At your stage, we focus on longevity, joint health, and sustainable energy. You're going to feel the difference."
                    age > 65 ->
                        "**$age** years of life experience! 🌳 That's incredible. We'll create a gentle but effective plan focused on strength, balance, and feeling your very best every day."
                    else ->
                        "Got it — **$age**! Let's move forward."
                }
                enqueueBot(ageReply)
                delay(300)
                enqueueBot("Now let's get your body measurements. Use the sliders below — drag to your exact height and weight. 📏")
                _state.value = _state.value.copy(showInput = false, showSliders = true, chips = emptyList())
            }

            else -> Unit
        }
    }

    fun confirmSliders(heightCm: Int, weightKg: Float) = viewModelScope.launch {
        val displayWeight = if (weightKg == weightKg.toInt().toFloat())
            "${weightKg.toInt()} kg"
        else
            "${"%.1f".format(weightKg)} kg"

        appendUser("Height: **$heightCm cm** · Weight: **$displayWeight**")

        val bmi = calculateBmi(heightCm, weightKg)
        val estimatedMonths = estimateMonths(bmi)

        val bmiCategory = when {
            bmi < 18.5f -> "underweight"
            bmi < 25f -> "normal"
            bmi < 30f -> "overweight"
            else -> "obese"
        }

        val verdict = when (bmiCategory) {
            "underweight" ->
                "Your BMI is **${"%.1f".format(bmi)}** — you're in the underweight zone. No worries at all! 🌱 We'll focus on building strong nutritional foundations and helping you gain in the healthiest way possible."
            "normal" ->
                "Your BMI is **${"%.1f".format(bmi)}** — you're right in the healthy range. Excellent! ✅ Now we'll focus on dialing in your body composition, energy levels, and long-term consistency."
            "overweight" ->
                "Your BMI is **${"%.1f".format(bmi)}** — slightly above the normal range. Nothing we can't fix together! 🔥 With CalixyAI, people in your exact range typically reach their target within 3–4 months."
            else ->
                "Your BMI is **${"%.1f".format(bmi)}** — above the recommended range right now. But here's the thing — every great journey starts exactly where you are. 💚 Step by step, we'll get you there."
        }

        val profile = _state.value.profile.copy(heightCm = heightCm, weightKg = weightKg, bmi = bmi)
        _state.value = _state.value.copy(
            profile = profile,
            step = ChatStep.ACTIVITY,
            showSliders = false,
            bmiUi = BmiUi(
                bmi = bmi,
                verdict = verdict,
                estimatedMonths = estimatedMonths,
                progress = (((bmi.coerceIn(15f, 35f) - 15f) / 20f) * 100).roundToInt()
            )
        )
        enqueueBot(verdict)
        _state.value = _state.value.copy(
            messages = _state.value.messages + ChatMessage(
                sender = Sender.BOT, text = "BMI", type = MessageType.BMI_CARD
            )
        )
        delay(300)
        enqueueBot("How would you describe your typical day? Be real with me — I'm not here to judge 😄")
        _state.value = _state.value.copy(
            showInput = false,
            chips = listOf("🛋️ Couch Potato", "🚶 Light Walker", "🏃 Moderately Active", "🏋️ Gym Regular", "⚡ Athlete Mode", "🧘 Yoga & Zen")
        )
    }

    private fun processSelection(value: String) = viewModelScope.launch {
        appendUser(value)
        when (_state.value.step) {
            ChatStep.GENDER -> {
                val profile = _state.value.profile.copy(gender = value)
                _state.value = _state.value.copy(
                    profile = profile, step = ChatStep.AGE,
                    showInput = true, chips = emptyList()
                )
                val response = when (value) {
                    "Male" ->
                        "A man with a plan. I respect that. 🫡 Let's build something powerful together."
                    "Female" ->
                        "Strong, determined, and ready to level up. ✨ I love that energy. Let's do this."
                    else ->
                        "Individuality is strength. 🌈 Your uniqueness is exactly what makes this plan special."
                }
                enqueueBot(response)
                delay(300)
                enqueueBot("How old are you? Age helps me calibrate your plan precisely. (Don't worry — it stays between us 🤫)")
            }

            ChatStep.ACTIVITY -> {
                val profile = _state.value.profile.copy(activityLevel = value)
                val activityReply = when {
                    value.contains("Couch") ->
                        "Honest answer — I love it! 😄 Starting from zero actually means the fastest early gains. We'll ease you in perfectly."
                    value.contains("Light") ->
                        "A light walker — that's a great foundation. 🚶 Consistency beats intensity every time, especially at the start."
                    value.contains("Moderate") ->
                        "Moderately active — solid base to build from! 🏃 You've already got the habit. Now we sharpen the fuel."
                    value.contains("Gym") ->
                        "Gym regular — now we're talking! 🏋️ You know the grind. We'll make sure your nutrition matches your effort."
                    value.contains("Athlete") ->
                        "Athlete mode! ⚡ Performance nutrition is a whole different game — and Calixy is built for exactly that."
                    value.contains("Yoga") ->
                        "Yoga & Zen — beautiful choice. 🧘 Mindful movement deserves mindful nutrition. We'll align them perfectly."
                    else -> "Great activity level! Let's keep going."
                }
                _state.value = _state.value.copy(
                    profile = profile,
                    step = ChatStep.GOAL,
                    chips = listOf(
                        "🔥 Lose Weight", "💪 Build Muscle",
                        "📊 Stay at My Weight", "🏃 Improve Fitness", "❤️ Just Be Healthier"
                    )
                )
                enqueueBot(activityReply)
                delay(300)
                enqueueBot("Now for the big question — what's the main mission? Pick your primary goal:")
            }

            ChatStep.GOAL -> {
                val profile = _state.value.profile.copy(goal = value)
                val goalReply = when {
                    value.contains("Lose") ->
                        "Locked in. 🔥 Fat loss done right — sustainable, smart, and without starving yourself. That's the Calixy way."
                    value.contains("Muscle") ->
                        "Building muscle it is! 💪 Gains take the right calories and the right timing. We'll nail both."
                    value.contains("Stay") ->
                        "Maintenance is seriously underrated. 📊 Keeping a healthy weight long-term takes real strategy. We've got you."
                    value.contains("Fitness") ->
                        "Fitness improvement — a goal that changes everything else too. 🏃 Energy, sleep, mood — all of it improves. Let's go!"
                    value.contains("Healthier") ->
                        "Just being healthier. ❤️ Honestly? That might be the smartest goal of all. We'll build lasting habits, not quick fixes."
                    else -> "Great goal! Let's build your plan around it."
                }
                _state.value = _state.value.copy(
                    profile = profile,
                    step = ChatStep.ALLERGIES,
                    chips = listOf(
                        "🥜 Peanuts", "🥛 Dairy", "🌾 Gluten", "🥚 Eggs",
                        "🐟 Seafood", "🍯 Soy", "🌰 Tree Nuts", "✏️ Custom…"
                    ),
                    multiSelect = true,
                    selectedItems = emptySet()
                )
                enqueueBot(goalReply)
                delay(300)
                enqueueBot("Almost there! Are there any foods your body just can't get along with? Select all that apply — or skip if you're allergy-free 👇")
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
                val allergyReply = when {
                    currentSelected.isEmpty() ->
                        "No allergies — lucky you! 🍀 That gives us maximum flexibility to design a truly varied and delicious plan."
                    currentSelected.size == 1 ->
                        "Noted — I'll keep **${currentSelected[0]}** completely out of your plan. You'll never even notice it's missing."
                    else ->
                        "Got it — **${currentSelected.joinToString(", ")}** are off the table. Don't worry, there's still a world of amazing food waiting for you. 🌍"
                }
                _state.value = _state.value.copy(
                    profile = _state.value.profile.copy(
                        allergies = currentSelected, customAllergy = customValue
                    ),
                    step = ChatStep.DIETARY,
                    chips = listOf(
                        "☪️ Halal", "✡️ Kosher", "🌱 Vegan", "🥗 Vegetarian",
                        "🥩 Keto", "🌾 Paleo", "🚫 No Restrictions"
                    ),
                    selectedItems = emptySet(),
                    multiSelect = true
                )
                enqueueBot(allergyReply)
                delay(300)
                enqueueBot("Last question, I promise! 🙏 Any dietary rules or lifestyles I should know about?")
            }

            ChatStep.DIETARY -> {
                val profile = _state.value.profile.copy(dietaryRules = currentSelected)
                val dietaryReply = when {
                    currentSelected.contains("🚫 No Restrictions") || currentSelected.isEmpty() ->
                        "No dietary restrictions — excellent! 🎯 That gives us the widest possible toolkit to build you an incredible, varied plan."
                    currentSelected.contains("🌱 Vegan") ->
                        "Vegan lifestyle — respect! 🌿 Plant-based nutrition done right is genuinely powerful. We'll make sure protein and micronutrients are perfectly covered."
                    currentSelected.contains("🥩 Keto") ->
                        "Keto! ⚡ High fat, low carb — when done correctly, it's remarkable for fat burning. We'll dial in your macros precisely."
                    else ->
                        "Perfect — **${currentSelected.joinToString(", ")}** noted. Your plan will respect every one of these preferences."
                }
                val finalAnalysis = buildFinalAnalysis(profile)
                _state.value = _state.value.copy(
                    profile = profile,
                    step = ChatStep.COMPLETE,
                    chips = emptyList(),
                    showInput = false,
                    selectedItems = emptySet(),
                    finalAnalysisUi = finalAnalysis
                )
                enqueueBot(dietaryReply)
                delay(500)
                enqueueBot("${profile.firstName}, I've crunched every number, cross-referenced your profile, and built your complete picture. 📊 Here it is:")
                _state.value = _state.value.copy(
                    messages = _state.value.messages + ChatMessage(
                        sender = Sender.BOT, text = "ANALYSIS", type = MessageType.ANALYSIS_CARD
                    )
                )
                delay(300)
                enqueueBot("This is your personalized CalixyAI roadmap — built around **you**, not a template. Ready to unlock the full plan? 🚀")
                repository.saveSetup(profile)
                _state.value = _state.value.copy(finished = true)
            }

            else -> Unit
        }
    }

    private suspend fun enqueueBot(text: String) {
        _state.value = _state.value.copy(
            messages = _state.value.messages + ChatMessage(
                sender = Sender.BOT, text = "typing", type = MessageType.TYPING
            )
        )
        // Typing delay scales with message length
        val typingMs = (700L + (text.length * 12L)).coerceIn(700L, 2200L)
        delay(typingMs)
        _state.value = _state.value.copy(
            messages = _state.value.messages.dropLast(1) + ChatMessage(
                sender = Sender.BOT, text = text
            )
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