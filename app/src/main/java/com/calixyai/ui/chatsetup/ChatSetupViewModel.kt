package com.calixyai.ui.chatsetup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calixyai.R
import com.calixyai.data.repository.AppRepository
import com.calixyai.domain.model.ChatMessage
import com.calixyai.domain.model.ChatStep
import com.calixyai.domain.model.MessageType
import com.calixyai.domain.model.Sender
import com.calixyai.domain.model.SetupProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val repository: AppRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ChatSetupState())
    val state: StateFlow<ChatSetupState> = _state.asStateFlow()

    // ── Language helpers ──────────────────────────────────────────────────────

    private val locale: String
        get() = context.getSharedPreferences("calixy_prefs", Context.MODE_PRIVATE)
            .getString("selected_locale", "en") ?: "en"

    private fun t(en: String, az: String = en, tr: String = en, ru: String = en): String =
        when (locale) { "az" -> az; "tr" -> tr; "ru" -> ru; else -> en }

    private fun str(@androidx.annotation.StringRes id: Int): String = context.getString(id)

    // ── Chip helpers ──────────────────────────────────────────────────────────

    private fun activityChips() = listOf(
        str(R.string.activity_couch),
        str(R.string.activity_walker),
        str(R.string.activity_moderate),
        str(R.string.activity_gym),
        str(R.string.activity_athlete),
        str(R.string.activity_yoga)
    )

    private fun goalChips() = listOf(
        str(R.string.goal_lose),
        str(R.string.goal_muscle),
        str(R.string.goal_maintain),
        str(R.string.goal_fitness),
        str(R.string.goal_health)
    )

    private fun allergyChips() = listOf(
        str(R.string.allergy_peanuts),
        str(R.string.allergy_dairy),
        str(R.string.allergy_gluten),
        str(R.string.allergy_eggs),
        str(R.string.allergy_seafood),
        str(R.string.allergy_soy),
        str(R.string.allergy_nuts),
        str(R.string.allergy_custom)
    )

    private fun dietaryChips() = listOf(
        str(R.string.dietary_halal),
        str(R.string.dietary_kosher),
        str(R.string.dietary_vegan),
        str(R.string.dietary_vegetarian),
        str(R.string.dietary_keto),
        str(R.string.dietary_paleo),
        str(R.string.dietary_none)
    )

    private fun genderChips() = listOf(
        str(R.string.gender_male),
        str(R.string.gender_female),
        str(R.string.gender_other)
    )

    private fun noRestrictionMarker() = str(R.string.dietary_none)
    private fun customMarker() = str(R.string.allergy_custom)

    // ── Intent dispatcher ─────────────────────────────────────────────────────

    fun onIntent(intent: ChatSetupIntent) {
        when (intent) {
            ChatSetupIntent.Initialize -> initialize()
            is ChatSetupIntent.SubmitText -> processText(intent.value)
            is ChatSetupIntent.SelectOption -> processSelection(intent.value)
            is ChatSetupIntent.ToggleMultiSelect -> toggleMulti(intent.value)
            is ChatSetupIntent.SubmitMultiSelect -> submitMulti(intent.customValue)
            is ChatSetupIntent.ConfirmSliders -> confirmSliders(intent.heightCm, intent.weightKg)
            is ChatSetupIntent.EditMessage -> editMessage(intent.messageId)
        }
    }

    // ── Initialize ────────────────────────────────────────────────────────────

    private fun initialize() {
        if (_state.value.messages.isNotEmpty()) return
        viewModelScope.launch {
            enqueueBot(
                t(
                    en = "Hey there! I'm Calixy, your personal AI nutrition coach 🤖\n\nWhat's your **first name**?",
                    az = "Salam! Mən Calixy, şəxsi AI qidalanma məşqçinizəm 🤖\n\n**Adınız** nədir?",
                    tr = "Merhaba! Ben Calixy, kişisel AI beslenme koçunuzum 🤖\n\n**Adınız** nedir?",
                    ru = "Привет! Я Calixy, ваш персональный AI-нутрициолог 🤖\n\nКак вас **зовут**?"
                )
            )
            _state.value = _state.value.copy(
                showInput = true,
                chips = emptyList(),
                requestKeyboard = true
            )
        }
    }

    // ── Edit: only allow editing the immediately previous user message ────────

    private fun editMessage(messageId: Long) {
        val messages = _state.value.messages
        val target = messages.firstOrNull { it.id == messageId } ?: return
        val editStep = target.editableStep ?: return

        // Find the last user message in the list
        val lastUserMsg = messages.lastOrNull { it.sender == Sender.USER }

        // Only allow editing the immediately previous (last) user message
        if (lastUserMsg?.id != messageId) return

        val idx = messages.indexOf(target)
        val trimmed = messages.take(idx)

        val profile = _state.value.profile
        val restoredProfile = when (editStep) {
            ChatStep.FIRST_NAME -> SetupProfile()
            ChatStep.LAST_NAME -> profile.copy(lastName = "")
            ChatStep.GENDER -> profile.copy(gender = "")
            ChatStep.AGE -> profile.copy(age = null)
            ChatStep.HEIGHT_WEIGHT -> profile.copy(heightCm = null, weightKg = null, bmi = null)
            ChatStep.ACTIVITY -> profile.copy(activityLevel = "")
            ChatStep.GOAL -> profile.copy(goal = "")
            ChatStep.TARGET_WEIGHT -> profile.copy(targetWeightKg = null)
            ChatStep.ALLERGIES -> profile.copy(allergies = emptyList(), customAllergy = null)
            ChatStep.DIETARY -> profile.copy(dietaryRules = emptyList())
            else -> profile
        }

        val (chips, multiSelect, showInput, showSliders) = when (editStep) {
            ChatStep.FIRST_NAME, ChatStep.LAST_NAME, ChatStep.AGE, ChatStep.TARGET_WEIGHT ->
                Quad(emptyList(), false, true, false)
            ChatStep.GENDER ->
                Quad(genderChips(), false, false, false)
            ChatStep.HEIGHT_WEIGHT ->
                Quad(emptyList(), false, false, true)
            ChatStep.ACTIVITY ->
                Quad(activityChips(), false, false, false)
            ChatStep.GOAL ->
                Quad(goalChips(), false, false, false)
            ChatStep.ALLERGIES ->
                Quad(allergyChips(), true, false, false)
            ChatStep.DIETARY ->
                Quad(dietaryChips(), true, false, false)
            else -> Quad(emptyList(), false, true, false)
        }

        _state.value = _state.value.copy(
            messages = trimmed,
            step = editStep,
            profile = restoredProfile,
            chips = chips,
            multiSelect = multiSelect,
            showInput = showInput,
            showSliders = showSliders,
            selectedItems = emptySet(),
            finished = false,
            bmiUi = if (editStep <= ChatStep.HEIGHT_WEIGHT) null else _state.value.bmiUi,
            finalAnalysisUi = null,
            requestKeyboard = showInput
        )
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    // ── Text steps ────────────────────────────────────────────────────────────

    private fun processText(value: String) = viewModelScope.launch {
        if (value.isBlank()) return@launch
        when (_state.value.step) {
            ChatStep.FIRST_NAME -> {
                val name = value.trim().uppercase()
                appendUser(value.trim(), ChatStep.FIRST_NAME)
                val profile = _state.value.profile.copy(firstName = value.trim())
                _state.value = _state.value.copy(
                    profile = profile,
                    step = ChatStep.LAST_NAME,
                    requestKeyboard = false
                )
                enqueueBot(
                    t(
                        en = "**$name!** Love that name 🎉 Now, what's your **surname**?",
                        az = "**$name!** Gözəl addır 🎉 İndi, **soyadınız** nədir?",
                        tr = "**$name!** Harika bir isim 🎉 Şimdi, **soyadınız** nedir?",
                        ru = "**$name!** Отличное имя 🎉 Теперь — какая у вас **фамилия**?"
                    )
                )
                _state.value = _state.value.copy(showInput = true, requestKeyboard = true)
            }

            ChatStep.LAST_NAME -> {
                val last = value.trim().uppercase()
                appendUser(value.trim(), ChatStep.LAST_NAME)
                val profile = _state.value.profile.copy(lastName = value.trim())
                val fullUpper = "${profile.firstName.uppercase()} $last"
                _state.value = _state.value.copy(profile = profile, step = ChatStep.GENDER)
                enqueueBot(
                    t(
                        en = "**$fullUpper** — a name that means business. Let's keep going! 💪",
                        az = "**$fullUpper** — bu ciddi bir addır. Davam edək! 💪",
                        tr = "**$fullUpper** — ciddi bir isim. Devam edelim! 💪",
                        ru = "**$fullUpper** — серьёзное имя. Продолжаем! 💪"
                    )
                )
                delay(350)
                enqueueBot(
                    t(
                        en = "How do you identify? This helps me personalize your plan perfectly.",
                        az = "Özünüzü necə təyin edirsiniz?",
                        tr = "Kendinizi nasıl tanımlıyorsunuz?",
                        ru = "Как вы себя идентифицируете?"
                    )
                )
                // Problem 3 fix: chips are shown AFTER the question is sent
                _state.value = _state.value.copy(
                    showInput = false,
                    chips = genderChips(),
                    multiSelect = false,
                    requestKeyboard = false
                )
            }

            ChatStep.AGE -> {
                val age = value.filter { it.isDigit() }.toIntOrNull() ?: 0
                appendUser("$age", ChatStep.AGE)
                val profile = _state.value.profile.copy(age = age)
                _state.value = _state.value.copy(
                    profile = profile,
                    step = ChatStep.HEIGHT_WEIGHT,
                    requestKeyboard = false
                )
                val ageReply = when {
                    age in 13..25 -> t(
                        en = "**$age** — peak energy years! ⚡ Your metabolism is your superpower.",
                        az = "**$age** — pik enerji illəri! ⚡ Metabolizmanız sizin super gücünüzdür.",
                        tr = "**$age** — zirve enerji yılları! ⚡ Metabolizmanız süper gücünüz.",
                        ru = "**$age** — пик энергии! ⚡ Ваш метаболизм — ваша суперсила."
                    )
                    age in 26..45 -> t(
                        en = "**$age**! Where discipline meets experience. 🔥",
                        az = "**$age**! İntizamın təcrübəylə buluşduğu yer. 🔥",
                        tr = "**$age**! Disiplinin deneyimle buluştuğu nokta. 🔥",
                        ru = "**$age**! Дисциплина встречается с опытом. 🔥"
                    )
                    else -> t(
                        en = "**$age** — and taking health seriously. 💪 Smart move.",
                        az = "**$age** — sağlamlığa ciddi yanaşırsınız. 💪",
                        tr = "**$age** — sağlığa ciddi yaklaşıyorsunuz. 💪",
                        ru = "**$age** — серьёзно относитесь к здоровью. 💪"
                    )
                }
                enqueueBot(ageReply)
                delay(300)
                enqueueBot(
                    t(
                        en = "Now let's get your measurements. Use the sliders below 📏",
                        az = "İndi bədən ölçülərinizi götürək. Aşağıdakı sürgülərdən istifadə edin 📏",
                        tr = "Şimdi vücut ölçülerinizi alalım. Aşağıdaki kaydırıcıları kullanın 📏",
                        ru = "Теперь замерим ваши параметры. Используйте слайдеры ниже 📏"
                    )
                )
                _state.value = _state.value.copy(
                    showInput = false,
                    showSliders = true,
                    chips = emptyList()
                )
            }

            // Problem 4: TARGET_WEIGHT step — text input with validation
            ChatStep.TARGET_WEIGHT -> {
                val currentWeight = _state.value.profile.weightKg ?: 70f
                val goal = _state.value.profile.goal
                val isLose = goal.contains("Lose") || goal.contains("Arıq") ||
                        goal.contains("Kilo Ver") || goal.contains("Похуд")
                val isGain = goal.contains("Muscle") || goal.contains("Əzələ") ||
                        goal.contains("Kas Yap") || goal.contains("мышц")

                val inputVal = value.replace(",", ".").trim().toFloatOrNull()
                if (inputVal == null || inputVal <= 0f) {
                    // Invalid — set hint error
                    _state.value = _state.value.copy(
                        inputError = str(R.string.target_weight_error_invalid)
                    )
                    return@launch
                }

                // Validation: lose → must be lower; gain → must be higher
                if (isLose && inputVal >= currentWeight) {
                    _state.value = _state.value.copy(
                        inputError = str(R.string.target_weight_error_lose)
                    )
                    return@launch
                }
                if (isGain && inputVal <= currentWeight) {
                    _state.value = _state.value.copy(
                        inputError = str(R.string.target_weight_error_gain)
                    )
                    return@launch
                }

                // Valid — clear error and proceed
                _state.value = _state.value.copy(inputError = null)

                val unit = t("kg", "kq", "kg", "кг")
                appendUser("${"%.1f".format(inputVal)} $unit", ChatStep.TARGET_WEIGHT)
                val profile = _state.value.profile.copy(targetWeightKg = inputVal)
                _state.value = _state.value.copy(
                    profile = profile,
                    step = ChatStep.ALLERGIES,
                    requestKeyboard = false
                )

                val diff = inputVal - currentWeight
                val months = estimateMonths(profile.bmi ?: 24f)
                val replyText = when {
                    isLose -> t(
                        en = "Great goal — losing ${"%.1f".format(-diff)} kg over $months months. 🔥 Consistent and sustainable!",
                        az = "$months ayda ${"%.1f".format(-diff)} kq arıqlamaq. 🔥 Sabit və davamlı!",
                        tr = "$months ayda ${"%.1f".format(-diff)} kg vermek. 🔥 Tutarlı ve sürdürülebilir!",
                        ru = "Сбросить ${"%.1f".format(-diff)} кг за $months мес. 🔥 Стабильно и устойчиво!"
                    )
                    isGain -> t(
                        en = "Awesome — gaining ${"%.1f".format(diff)} kg over $months months. 💪 Smart muscle-building plan!",
                        az = "$months ayda ${"%.1f".format(diff)} kq qazanmaq. 💪 Ağıllı əzələ qurmaq planı!",
                        tr = "$months ayda ${"%.1f".format(diff)} kg almak. 💪 Akıllı kas yapım planı!",
                        ru = "Набрать ${"%.1f".format(diff)} кг за $months мес. 💪 Отличный план!"
                    )
                    else -> t(
                        en = "Maintaining around ${"%.1f".format(inputVal)} kg. 📊 Consistency is the real superpower.",
                        az = "${"%.1f".format(inputVal)} kq ətrafında qorumaq. 📊",
                        tr = "${"%.1f".format(inputVal)} kg civarında korumak. 📊",
                        ru = "Поддерживать ${"%.1f".format(inputVal)} кг. 📊"
                    )
                }
                enqueueBot(replyText)
                delay(300)
                enqueueBot(
                    t(
                        en = "Almost there! Any foods your body can't get along with? Select all that apply 👇",
                        az = "Az qalır! Bədəninizin uyğunlaşa bilmədiyi yeməklər varmı? 👇",
                        tr = "Neredeyse bitti! Vücudunuzun geçinemediği yiyecekler var mı? 👇",
                        ru = "Почти готово! Есть продукты, с которыми организм не ладит? 👇"
                    )
                )
                // Problem 3 fix: chips shown AFTER the question bot message
                _state.value = _state.value.copy(
                    showInput = false,
                    chips = allergyChips(),
                    multiSelect = true,
                    selectedItems = emptySet()
                )
            }

            else -> Unit
        }
    }

    // ── Slider confirm ────────────────────────────────────────────────────────

    fun confirmSliders(heightCm: Int, weightKg: Float) = viewModelScope.launch {
        val displayWeight = if (weightKg == weightKg.toInt().toFloat())
            "${weightKg.toInt()} ${t("kg", "kq", "kg", "кг")}"
        else "${"%.1f".format(weightKg)} ${t("kg", "kq", "kg", "кг")}"

        appendUser(
            t(
                en = "Height: $heightCm cm · Weight: $displayWeight",
                az = "Boy: $heightCm sm · Çəki: $displayWeight",
                tr = "Boy: $heightCm cm · Ağırlık: $displayWeight",
                ru = "Рост: $heightCm см · Вес: $displayWeight"
            ),
            ChatStep.HEIGHT_WEIGHT
        )

        val bmi = calculateBmi(heightCm, weightKg)
        val estimatedMonths = estimateMonths(bmi)
        val bmiCategory = when {
            bmi < 18.5f -> "underweight"; bmi < 25f -> "normal"
            bmi < 30f -> "overweight"; else -> "obese"
        }

        val verdict = when (bmiCategory) {
            "underweight" -> t(
                en = "Your BMI is **${"%.1f".format(bmi)}** — underweight zone. 🌱 We'll help you build strong foundations.",
                az = "BKİ-niz **${"%.1f".format(bmi)}** — az çəkili zona. 🌱 Güclü əsaslar qurmağa kömək edəcəyik.",
                tr = "VKİ'niz **${"%.1f".format(bmi)}** — zayıf bölge. 🌱 Güçlü temeller oluşturmanıza yardımcı olacağız.",
                ru = "Ваш ИМТ **${"%.1f".format(bmi)}** — зона недостаточного веса. 🌱 Поможем выстроить крепкую основу."
            )
            "normal" -> t(
                en = "Your BMI is **${"%.1f".format(bmi)}** — healthy range. ✅ Excellent! Let's dial in your composition and energy.",
                az = "BKİ-niz **${"%.1f".format(bmi)}** — sağlam aralıq. ✅ Əla!",
                tr = "VKİ'niz **${"%.1f".format(bmi)}** — sağlıklı aralık. ✅ Mükemmel!",
                ru = "Ваш ИМТ **${"%.1f".format(bmi)}** — здоровая норма. ✅ Отлично!"
            )
            "overweight" -> t(
                en = "Your BMI is **${"%.1f".format(bmi)}** — slightly above normal. 🔥 Nothing we can't work on together!",
                az = "BKİ-niz **${"%.1f".format(bmi)}** — normalın bir az üstündədir. 🔥",
                tr = "VKİ'niz **${"%.1f".format(bmi)}** — normal aralığın biraz üzerinde. 🔥",
                ru = "Ваш ИМТ **${"%.1f".format(bmi)}** — немного выше нормы. 🔥 Вместе разберёмся!"
            )
            else -> t(
                en = "Your BMI is **${"%.1f".format(bmi)}** — above recommended range. 💚 Every great journey starts exactly here.",
                az = "BKİ-niz **${"%.1f".format(bmi)}** — tövsiyə edilən aralığın üstündədir. 💚",
                tr = "VKİ'niz **${"%.1f".format(bmi)}** — önerilen aralığın üzerinde. 💚",
                ru = "Ваш ИМТ **${"%.1f".format(bmi)}** — выше рекомендуемого. 💚"
            )
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
        enqueueBot(
            t(
                en = "How would you describe your typical day? Be real with me 😄",
                az = "Tipik gününüzü necə təsvir edərdiniz? 😄",
                tr = "Tipik gününüzü nasıl tanımlarsınız? 😄",
                ru = "Как бы вы описали свой обычный день? 😄"
            )
        )
        // Problem 3 fix: chips AFTER bot question
        _state.value = _state.value.copy(showInput = false, chips = activityChips())
    }

    // ── Single-select chips ───────────────────────────────────────────────────

    private fun processSelection(value: String) = viewModelScope.launch {
        appendUser(value, _state.value.step)
        when (_state.value.step) {

            ChatStep.GENDER -> {
                val profile = _state.value.profile.copy(gender = value)
                _state.value = _state.value.copy(
                    profile = profile, step = ChatStep.AGE,
                    showInput = true, chips = emptyList(), requestKeyboard = true
                )
                val isMale = value.contains("Male") || value.contains("Erkek") || value.contains("Kişi") || value.contains("Мужчина")
                val isFemale = value.contains("Female") || value.contains("Kadın") || value.contains("Qadın") || value.contains("Женщина")
                val response = when {
                    isMale -> t(
                        en = "A man with a plan. I respect that. 🫡 Let's build something powerful together.",
                        az = "Planı olan bir kişi. 🫡 Birlikdə güclü bir şey quracağıq.",
                        tr = "Planı olan bir adam. 🫡 Birlikte güçlü bir şey inşa edelim.",
                        ru = "Мужчина с планом. 🫡 Вместе построим что-то мощное."
                    )
                    isFemale -> t(
                        en = "Strong, determined, and ready to level up. ✨ Let's do this.",
                        az = "Güclü, qətiyyətli. ✨ Başlayaq.",
                        tr = "Güçlü, kararlı. ✨ Hadi başlayalım.",
                        ru = "Сильная и решительная. ✨ Поехали."
                    )
                    else -> t(
                        en = "Individuality is strength. 🌈 Your uniqueness makes this plan special.",
                        az = "Fərdiyyət gücdür. 🌈",
                        tr = "Bireysellik güçtür. 🌈",
                        ru = "Индивидуальность — сила. 🌈"
                    )
                }
                enqueueBot(response)
                delay(300)
                enqueueBot(
                    t(
                        en = "How old are you? (Don't worry — just between us 🤫)",
                        az = "Neçə yaşınız var? (Narahat olmayın — aramızda qalır 🤫)",
                        tr = "Kaç yaşındasınız? (Merak etmeyin — aramızda kalır 🤫)",
                        ru = "Сколько вам лет? (Останется между нами 🤫)"
                    )
                )
            }

            ChatStep.ACTIVITY -> {
                val profile = _state.value.profile.copy(activityLevel = value)
                val activityReply = when {
                    value.contains("Couch") || value.contains("Divan") || value.contains("Kanepe") || value.contains("Диванный") ->
                        t(en = "Honest answer — love it! 😄 Starting from zero means fastest early gains.",
                            az = "Dürüst cavab! 😄 Sıfırdan başlamaq ən sürətli nəticədir.",
                            tr = "Dürüst cevap! 😄 Sıfırdan başlamak hızlı kazanımlar demek.",
                            ru = "Честно — обожаю! 😄 Начать с нуля — самые быстрые результаты.")
                    value.contains("Light") || value.contains("Yüngül") || value.contains("Hafif") || value.contains("Лёгкая") ->
                        t(en = "Light walker — great foundation. 🚶 Consistency beats intensity.",
                            az = "Yüngül gəzən — əla əsas. 🚶",
                            tr = "Hafif yürüyüşçü — harika temel. 🚶",
                            ru = "Лёгкая ходьба — отличная база. 🚶")
                    value.contains("Moderate") || value.contains("Orta") || value.contains("Умеренно") ->
                        t(en = "Moderately active — solid base! 🏃 You already have the habit.",
                            az = "Orta aktiv — güclü əsas! 🏃",
                            tr = "Orta aktif — sağlam temel! 🏃",
                            ru = "Умеренная активность — крепкая база! 🏃")
                    value.contains("Gym") || value.contains("Zal") || value.contains("Spor") || value.contains("Постоянный") ->
                        t(en = "Gym regular — now we're talking! 🏋️ Nutrition must match your effort.",
                            az = "Zal daimi! 🏋️",
                            tr = "Spor salonu düzenlisi! 🏋️",
                            ru = "Постоянный в зале! 🏋️")
                    value.contains("Athlete") || value.contains("Atlet") || value.contains("атлета") ->
                        t(en = "Athlete mode! ⚡ Performance nutrition is a different game.",
                            az = "Atlet rejimi! ⚡",
                            tr = "Atlet modu! ⚡",
                            ru = "Режим атлета! ⚡")
                    else ->
                        t(en = "Yoga & Zen — beautiful choice. 🧘 Mindful movement deserves mindful nutrition.",
                            az = "Yoqa & Zen — gözəl seçim. 🧘",
                            tr = "Yoga & Zen — güzel seçim. 🧘",
                            ru = "Йога и дзен — прекрасный выбор. 🧘")
                }
                _state.value = _state.value.copy(profile = profile, step = ChatStep.GOAL, chips = emptyList())
                enqueueBot(activityReply)
                delay(300)
                enqueueBot(
                    t(
                        en = "Now for the big question — what's the main mission?",
                        az = "İndi böyük sual — əsas missiya nədir?",
                        tr = "Şimdi büyük soru — ana misyon nedir?",
                        ru = "Теперь главный вопрос — в чём главная миссия?"
                    )
                )
                // Problem 3 fix: chips AFTER the question
                _state.value = _state.value.copy(chips = goalChips())
            }

            ChatStep.GOAL -> {
                val profile = _state.value.profile.copy(goal = value)
                val isWeightRelated = value.contains("Lose") || value.contains("Arıq") || value.contains("Kilo Ver") ||
                        value.contains("Похуд") || value.contains("Muscle") || value.contains("Əzələ") ||
                        value.contains("Kas Yap") || value.contains("мышц")

                val goalReply = when {
                    value.contains("Lose") || value.contains("Arıq") || value.contains("Kilo Ver") || value.contains("Похуд") ->
                        t(en = "Locked in. 🔥 Smart, sustainable fat loss — the Calixy way.",
                            az = "Qəbul edildi. 🔥 Davamlı, ağıllı yağ itkisi — Calixy üslubu.",
                            tr = "Kilitlendi. 🔥 Akıllı, sürdürülebilir yağ kaybı.",
                            ru = "Принято. 🔥 Умное, устойчивое жиросжигание.")
                    value.contains("Muscle") || value.contains("Əzələ") || value.contains("Kas") || value.contains("мышц") ->
                        t(en = "Building muscle! 💪 The right calories at the right time — we'll nail both.",
                            az = "Əzələ qurmaq! 💪 Doğru kalorilər, doğru vaxtlama.",
                            tr = "Kas yapmak! 💪 Doğru kalori, doğru zamanlama.",
                            ru = "Набор мышц! 💪 Правильные калории в правильное время.")
                    value.contains("Stay") || value.contains("Çəkimi") || value.contains("Kilomu") || value.contains("Сохран") ->
                        t(en = "Maintenance is seriously underrated. 📊 Long-term strategy is the real game.",
                            az = "Qoruma ciddi qiymətləndirilmir. 📊",
                            tr = "Koruma ciddi şekilde küçümseniyor. 📊",
                            ru = "Поддержание веса серьёзно недооценивается. 📊")
                    else ->
                        t(en = "A goal that changes everything. 🏃 Energy, sleep, mood — all improve.",
                            az = "Hər şeyi dəyişdirən hədəf. 🏃",
                            tr = "Her şeyi değiştiren hedef. 🏃",
                            ru = "Цель, меняющая всё. 🏃")
                }

                enqueueBot(goalReply)
                delay(350)

                if (isWeightRelated) {
                    // Problem 4: ask for target weight via text input — no chips
                    val currentWeight = _state.value.profile.weightKg ?: 70f
                    val isLose = value.contains("Lose") || value.contains("Arıq") || value.contains("Kilo Ver") || value.contains("Похуд")
                    val hintText = if (isLose) {
                        t(
                            en = "What weight do you want to reach? (must be lower than ${"%.1f".format(currentWeight)} kg)",
                            az = "Hansı çəkiyə çatmaq istəyirsiniz? (${"%.1f".format(currentWeight)} kq-dan az olmalıdır)",
                            tr = "Hangi kiloya ulaşmak istiyorsunuz? (${"%.1f".format(currentWeight)} kg'dan düşük olmalı)",
                            ru = "Какого веса хотите достичь? (должен быть ниже ${"%.1f".format(currentWeight)} кг)"
                        )
                    } else {
                        t(
                            en = "What weight do you want to reach? (must be higher than ${"%.1f".format(currentWeight)} kg)",
                            az = "Hansı çəkiyə çatmaq istəyirsiniz? (${"%.1f".format(currentWeight)} kq-dan çox olmalıdır — yuxarı çəki yazın)",
                            tr = "Hangi kiloya ulaşmak istiyorsunuz? (${"%.1f".format(currentWeight)} kg'dan yüksek olmalı — hedef kilo yazın)",
                            ru = "Какого веса хотите достичь? (должен быть выше ${"%.1f".format(currentWeight)} кг)"
                        )
                    }
                    _state.value = _state.value.copy(
                        profile = profile,
                        step = ChatStep.TARGET_WEIGHT,
                        chips = emptyList(),
                        multiSelect = false,
                        inputHint = hintText
                    )
                    enqueueBot(hintText)
                    _state.value = _state.value.copy(
                        showInput = true,
                        requestKeyboard = true
                    )
                } else {
                    // Skip target weight, go straight to allergies
                    _state.value = _state.value.copy(
                        profile = profile,
                        step = ChatStep.ALLERGIES,
                        chips = emptyList(),
                        multiSelect = true,
                        selectedItems = emptySet()
                    )
                    enqueueBot(
                        t(
                            en = "Almost there! Any foods your body can't get along with? Select all that apply 👇",
                            az = "Az qalır! Bədəninizin uyğunlaşa bilmədiyi yeməklər varmı? 👇",
                            tr = "Neredeyse bitti! Vücudunuzun geçinemediği yiyecekler var mı? 👇",
                            ru = "Почти готово! Есть продукты, с которыми организм не ладит? 👇"
                        )
                    )
                    // Problem 3: chips after question
                    _state.value = _state.value.copy(
                        showInput = false,
                        chips = allergyChips()
                    )
                }
            }

            else -> Unit
        }
    }

    // ── Multi-select with No Restrictions logic ───────────────────────────────

    private fun toggleMulti(value: String) {
        val set = _state.value.selectedItems.toMutableSet()
        val noRestriction = noRestrictionMarker()
        val customKey = customMarker()

        when {
            value == noRestriction -> {
                if (set.contains(noRestriction)) set.remove(noRestriction)
                else { set.clear(); set.add(noRestriction) }
            }
            else -> {
                set.remove(noRestriction)
                if (set.contains(value)) set.remove(value) else set.add(value)
            }
        }

        val showCustom = set.contains(customKey)
        _state.value = _state.value.copy(
            selectedItems = set,
            showCustomInput = showCustom,
            requestKeyboard = showCustom
        )
    }

    // ── Multi-select submit ───────────────────────────────────────────────────

    private fun submitMulti(customValue: String?) = viewModelScope.launch {
        val currentSelected = _state.value.selectedItems
            .filterNot { it == customMarker() }
            .toMutableList()
        if (!customValue.isNullOrBlank()) currentSelected.add(customValue)

        appendUser(
            currentSelected.joinToString(" · ").ifBlank { t("None", "Heç biri", "Hiçbiri", "Нет") },
            _state.value.step
        )

        when (_state.value.step) {
            ChatStep.ALLERGIES -> {
                val allergyReply = when {
                    currentSelected.isEmpty() -> t(
                        en = "No allergies — lucky you! 🍀 Maximum flexibility to design a varied, delicious plan.",
                        az = "Allergiya yoxdur! 🍀 Müxtəlif və dadlı plan hazırlamaq üçün maksimum çeviklik.",
                        tr = "Alerji yok — şanslısınız! 🍀 Çeşitli, lezzetli plan tasarlamak için maksimum esneklik.",
                        ru = "Нет аллергий — вам повезло! 🍀 Максимальная гибкость для разнообразного плана."
                    )
                    else -> t(
                        en = "Got it — ${currentSelected.joinToString(", ")} are off the table. 🌍 There's still a world of amazing food for you.",
                        az = "Qəbul edildi — ${currentSelected.joinToString(", ")} istisna edildi. 🌍",
                        tr = "Tamam — ${currentSelected.joinToString(", ")} devre dışı. 🌍",
                        ru = "Понял — ${currentSelected.joinToString(", ")} исключены. 🌍"
                    )
                }
                _state.value = _state.value.copy(
                    profile = _state.value.profile.copy(
                        allergies = currentSelected, customAllergy = customValue
                    ),
                    step = ChatStep.DIETARY,
                    chips = emptyList(),
                    selectedItems = emptySet(),
                    multiSelect = true,
                    showCustomInput = false
                )
                enqueueBot(allergyReply)
                delay(300)
                enqueueBot(
                    t(
                        en = "Last question, I promise! 🙏 Any dietary rules or lifestyles I should know about?",
                        az = "Son sual, söz verirəm! 🙏 Bilməli olduğum pəhriz qaydaları varmı?",
                        tr = "Son soru, söz veriyorum! 🙏 Bilmem gereken diyet kuralı var mı?",
                        ru = "Последний вопрос, обещаю! 🙏 Есть диетические правила или образ жизни?"
                    )
                )
                // Problem 3: chips after question
                _state.value = _state.value.copy(chips = dietaryChips())
            }

            ChatStep.DIETARY -> {
                val profile = _state.value.profile.copy(dietaryRules = currentSelected)
                val hasNoRestriction = currentSelected.any {
                    it.contains("No Restrictions") || it.contains("Məhdudiyyət") ||
                            it.contains("Kısıtlama") || it.contains("Без ограничений")
                }
                val isVegan = currentSelected.any { it.contains("Vegan") || it.contains("Веган") }
                val isKeto = currentSelected.any { it.contains("Keto") || it.contains("Кето") }

                val dietaryReply = when {
                    hasNoRestriction || currentSelected.isEmpty() -> t(
                        en = "No dietary restrictions — excellent! 🎯 Widest possible toolkit for an incredible plan.",
                        az = "Pəhriz məhdudiyyəti yoxdur! 🎯",
                        tr = "Diyet kısıtlaması yok — mükemmel! 🎯",
                        ru = "Никаких ограничений — отлично! 🎯"
                    )
                    isVegan -> t(
                        en = "Vegan lifestyle — respect! 🌿 Plant-based nutrition done right is genuinely powerful.",
                        az = "Vegan həyat tərzi! 🌿",
                        tr = "Vegan yaşam tarzı — saygı! 🌿",
                        ru = "Веганский образ жизни — уважаю! 🌿"
                    )
                    isKeto -> t(
                        en = "Keto! ⚡ High fat, low carb — remarkable for fat burning when done correctly.",
                        az = "Keto! ⚡",
                        tr = "Keto! ⚡",
                        ru = "Кето! ⚡"
                    )
                    else -> t(
                        en = "Perfect — ${currentSelected.joinToString(", ")} noted. Your plan will respect every preference.",
                        az = "Mükəmməl — ${currentSelected.joinToString(", ")} qeyd edildi.",
                        tr = "Mükemmel — ${currentSelected.joinToString(", ")} not alındı.",
                        ru = "Отлично — ${currentSelected.joinToString(", ")} отмечено."
                    )
                }

                val finalAnalysis = buildFinalAnalysis(profile)
                _state.value = _state.value.copy(
                    profile = profile,
                    step = ChatStep.COMPLETE,
                    chips = emptyList(),
                    showInput = false,
                    selectedItems = emptySet(),
                    finalAnalysisUi = finalAnalysis,
                    showCustomInput = false
                )
                enqueueBot(dietaryReply)
                delay(500)
                enqueueBot(
                    t(
                        en = "${profile.firstName.uppercase()}, I've crunched every number and built your complete picture. 📊 Here it is:",
                        az = "${profile.firstName.uppercase()}, hər rəqəmi hesabladım. 📊 Budur:",
                        tr = "${profile.firstName.uppercase()}, her sayıyı hesapladım. 📊 İşte:",
                        ru = "${profile.firstName.uppercase()}, я проанализировал каждую цифру. 📊 Вот она:"
                    )
                )
                _state.value = _state.value.copy(
                    messages = _state.value.messages + ChatMessage(
                        sender = Sender.BOT, text = "ANALYSIS", type = MessageType.ANALYSIS_CARD
                    )
                )
                delay(300)
                enqueueBot(
                    t(
                        en = "This is your personalized CalixyAI roadmap — built around **you**. Ready to unlock the full plan? 🚀",
                        az = "Bu sizin fərdi CalixyAI yol xəritənizdir. Tam planı açmağa hazırsınız? 🚀",
                        tr = "Bu sizin kişiselleştirilmiş CalixyAI yol haritanız. Tam planı açmaya hazır mısınız? 🚀",
                        ru = "Это ваша персональная дорожная карта CalixyAI. Готовы открыть полный план? 🚀"
                    )
                )
                repository.saveSetup(profile)
                _state.value = _state.value.copy(finished = true)
            }

            else -> Unit
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun enqueueBot(text: String) {
        _state.value = _state.value.copy(
            messages = _state.value.messages + ChatMessage(
                sender = Sender.BOT, text = "typing", type = MessageType.TYPING
            )
        )
        val typingMs = (700L + (text.length * 12L)).coerceIn(700L, 2200L)
        delay(typingMs)
        _state.value = _state.value.copy(
            messages = _state.value.messages.dropLast(1) + ChatMessage(
                sender = Sender.BOT, text = text
            )
        )
    }

    private fun appendUser(text: String, step: ChatStep) {
        _state.value = _state.value.copy(
            messages = _state.value.messages + ChatMessage(
                sender = Sender.USER,
                text = text,
                editableStep = step
            )
        )
    }

    private fun calculateBmi(heightCm: Int, weightKg: Float): Float {
        val meters = heightCm / 100f
        return (weightKg / meters.pow(2)).let { (it * 10).roundToInt() / 10f }
    }

    private fun estimateMonths(bmi: Float): Int = when {
        bmi < 18.5f -> 3; bmi < 25f -> 2; bmi < 30f -> 4; else -> 6
    }

    // Problem 4: buildFinalAnalysis uses targetWeightKg from profile
    private fun buildFinalAnalysis(profile: SetupProfile): FinalAnalysisUi {
        val currentWeight = profile.weightKg ?: 70f
        val targetWeight = profile.targetWeightKg ?: when {
            profile.goal.contains("Lose") || profile.goal.contains("Arıq") ||
                    profile.goal.contains("Kilo Ver") || profile.goal.contains("Похуд") ->
                currentWeight - 8f
            profile.goal.contains("Muscle") || profile.goal.contains("Əzələ") ||
                    profile.goal.contains("Kas") || profile.goal.contains("мышц") ->
                currentWeight + 4f
            else -> currentWeight - 2f
        }
        val months = estimateMonths(profile.bmi ?: 24f)
        val dailyCalories = when {
            targetWeight < currentWeight -> 1900
            targetWeight > currentWeight -> 2500
            else -> 2150
        }
        val heightM = (profile.heightCm ?: 170) / 100f
        val targetBmi = ((targetWeight / heightM.pow(2)) * 10).roundToInt() / 10f
        // Chart: two points — current weight and target weight
        val points = listOf(
            ChartPoint(0f, currentWeight),
            ChartPoint(months.toFloat(), targetWeight)
        ) + (1 until months).map { month ->
            val progress = month / months.toFloat()
            ChartPoint(month.toFloat(), currentWeight + ((targetWeight - currentWeight) * progress))
        }.sortedBy { it.month }

        // Build smooth curve: start → intermediate → end
        val smoothPoints = (0..months).map { month ->
            val progress = month / months.toFloat().coerceAtLeast(1f)
            ChartPoint(month.toFloat(), currentWeight + ((targetWeight - currentWeight) * progress))
        }

        return FinalAnalysisUi(
            currentBmi = profile.bmi ?: 0f,
            targetBmi = targetBmi,
            estimatedDuration = months,
            dailyCalories = dailyCalories,
            chartPoints = smoothPoints,
            currentWeight = currentWeight,
            targetWeight = targetWeight
        )
    }
}

// Helper operator for ChatStep comparison
private operator fun ChatStep.compareTo(other: ChatStep): Int = this.ordinal.compareTo(other.ordinal)