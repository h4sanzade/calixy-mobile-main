package com.calixyai.ui.chatsetup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    // ── Chip helpers ──────────────────────────────────────────────────────────

    private fun activityChips() = when (locale) {
        "az" -> listOf("🛋️ Divan Sevəni", "🚶 Yüngül Gəzən", "🏃 Orta Aktiv", "🏋️ Zal Daimi", "⚡ Atlet Rejimi", "🧘 Yoqa & Zen")
        "tr" -> listOf("🛋️ Kanepe Tutkunu", "🚶 Hafif Yürüyüşçü", "🏃 Orta Aktif", "🏋️ Spor Salonu Düzenlisi", "⚡ Atlet Modu", "🧘 Yoga & Zen")
        "ru" -> listOf("🛋️ Диванный эксперт", "🚶 Лёгкая ходьба", "🏃 Умеренно активный", "🏋️ Постоянный в зале", "⚡ Режим атлета", "🧘 Йога и дзен")
        else -> listOf("🛋️ Couch Potato", "🚶 Light Walker", "🏃 Moderately Active", "🏋️ Gym Regular", "⚡ Athlete Mode", "🧘 Yoga & Zen")
    }

    private fun goalChips() = when (locale) {
        "az" -> listOf("🔥 Arıqla", "💪 Əzələ Qur", "📊 Çəkimi Saxla", "🏃 Formanı Yaxşılaşdır", "❤️ Sadəcə Sağlam Ol")
        "tr" -> listOf("🔥 Kilo Ver", "💪 Kas Yap", "📊 Kilomu Koru", "🏃 Formu Geliştir", "❤️ Sadece Sağlıklı Ol")
        "ru" -> listOf("🔥 Похудеть", "💪 Набрать мышцы", "📊 Сохранить вес", "🏃 Улучшить форму", "❤️ Просто быть здоровым")
        else -> listOf("🔥 Lose Weight", "💪 Build Muscle", "📊 Stay at My Weight", "🏃 Improve Fitness", "❤️ Just Be Healthier")
    }

    private fun weightDirectionChips() = when (locale) {
        "az" -> listOf("⬇️ Arıqlamaq istəyirəm", "⬆️ Çəki qazanmaq istəyirəm", "↔️ Sabit saxlamaq istəyirəm")
        "tr" -> listOf("⬇️ Kilo vermek istiyorum", "⬆️ Kilo almak istiyorum", "↔️ Sabit tutmak istiyorum")
        "ru" -> listOf("⬇️ Хочу похудеть", "⬆️ Хочу набрать вес", "↔️ Хочу остаться таким же")
        else -> listOf("⬇️ I want to lose weight", "⬆️ I want to gain weight", "↔️ I want to stay the same")
    }

    private fun allergyChips() = when (locale) {
        "az" -> listOf("🥜 Yer fındığı", "🥛 Süd məhsulları", "🌾 Qluten", "🥚 Yumurta", "🐟 Dəniz məhsulları", "🍯 Soya", "🌰 Ağac qozları", "✏️ Özüm yazım…")
        "tr" -> listOf("🥜 Yer Fıstığı", "🥛 Süt Ürünleri", "🌾 Gluten", "🥚 Yumurta", "🐟 Deniz Ürünleri", "🍯 Soya", "🌰 Ağaç Kuruyemişi", "✏️ Özel…")
        "ru" -> listOf("🥜 Арахис", "🥛 Молочные продукты", "🌾 Глютен", "🥚 Яйца", "🐟 Морепродукты", "🍯 Соя", "🌰 Древесные орехи", "✏️ Своё…")
        else -> listOf("🥜 Peanuts", "🥛 Dairy", "🌾 Gluten", "🥚 Eggs", "🐟 Seafood", "🍯 Soy", "🌰 Tree Nuts", "✏️ Custom…")
    }

    private fun dietaryChips() = when (locale) {
        "az" -> listOf("☪️ Halal", "✡️ Koşer", "🌱 Vegan", "🥗 Vegetarian", "🥩 Keto", "🌾 Paleo", "🚫 Məhdudiyyət Yoxdur")
        "tr" -> listOf("☪️ Helal", "✡️ Koşer", "🌱 Vegan", "🥗 Vejetaryen", "🥩 Keto", "🌾 Paleo", "🚫 Kısıtlama Yok")
        "ru" -> listOf("☪️ Халяль", "✡️ Кошерное", "🌱 Веган", "🥗 Вегетарианец", "🥩 Кето", "🌾 Палео", "🚫 Без ограничений")
        else -> listOf("☪️ Halal", "✡️ Kosher", "🌱 Vegan", "🥗 Vegetarian", "🥩 Keto", "🌾 Paleo", "🚫 No Restrictions")
    }

    private fun genderChips() = when (locale) {
        "az" -> listOf("👨 Kişi", "👩 Qadın", "🌈 Digər")
        "tr" -> listOf("👨 Erkek", "👩 Kadın", "🌈 Diğer")
        "ru" -> listOf("👨 Мужчина", "👩 Женщина", "🌈 Другое")
        else -> listOf("👨 Male", "👩 Female", "🌈 Other")
    }

    private fun noRestrictionMarker() = when (locale) {
        "az" -> "🚫 Məhdudiyyət Yoxdur"; "tr" -> "🚫 Kısıtlama Yok"
        "ru" -> "🚫 Без ограничений"; else -> "🚫 No Restrictions"
    }

    private fun customMarker() = when (locale) {
        "az" -> "✏️ Özüm yazım…"; "tr" -> "✏️ Özel…"
        "ru" -> "✏️ Своё…"; else -> "✏️ Custom…"
    }

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

    // ── Edit a past message ───────────────────────────────────────────────────

    private fun editMessage(messageId: Long) {
        val messages = _state.value.messages
        val target = messages.firstOrNull { it.id == messageId } ?: return
        val editStep = target.editableStep ?: return

        // Remove all messages from this message onward (user + subsequent bot)
        val idx = messages.indexOf(target)
        val trimmed = messages.take(idx)

        // Restore profile state to before this step was answered
        val profile = _state.value.profile
        val restoredProfile = when (editStep) {
            ChatStep.FIRST_NAME -> SetupProfile()
            ChatStep.LAST_NAME -> profile.copy(lastName = "")
            ChatStep.GENDER -> profile.copy(gender = "")
            ChatStep.AGE -> profile.copy(age = null)
            ChatStep.HEIGHT_WEIGHT -> profile.copy(heightCm = null, weightKg = null, bmi = null)
            ChatStep.ACTIVITY -> profile.copy(activityLevel = "")
            ChatStep.GOAL -> profile.copy(goal = "")
            ChatStep.WEIGHT_DIRECTION -> profile.copy(weightDirection = "")
            ChatStep.ALLERGIES -> profile.copy(allergies = emptyList(), customAllergy = null)
            ChatStep.DIETARY -> profile.copy(dietaryRules = emptyList())
            else -> profile
        }

        // Determine what UI to show for re-entry
        val (chips, multiSelect, showInput, showSliders) = when (editStep) {
            ChatStep.FIRST_NAME, ChatStep.LAST_NAME, ChatStep.AGE ->
                Quad(emptyList(), false, true, false)
            ChatStep.GENDER ->
                Quad(genderChips(), false, false, false)
            ChatStep.HEIGHT_WEIGHT ->
                Quad(emptyList(), false, false, true)
            ChatStep.ACTIVITY ->
                Quad(activityChips(), false, false, false)
            ChatStep.GOAL ->
                Quad(goalChips(), false, false, false)
            ChatStep.WEIGHT_DIRECTION ->
                Quad(weightDirectionChips(), false, false, false)
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
                az = "BKİ-niz **${"%.1f".format(bmi)}** — sağlam aralıq. ✅ Əla! Bədən kompozisiyası və enerjisinə diqqət edəcəyik.",
                tr = "VKİ'niz **${"%.1f".format(bmi)}** — sağlıklı aralık. ✅ Mükemmel!",
                ru = "Ваш ИМТ **${"%.1f".format(bmi)}** — здоровая норма. ✅ Отлично!"
            )
            "overweight" -> t(
                en = "Your BMI is **${"%.1f".format(bmi)}** — slightly above normal. 🔥 Nothing we can't work on together!",
                az = "BKİ-niz **${"%.1f".format(bmi)}** — normalın bir az üstündədir. 🔥 Birlikdə üzərində işləyə biləcəyimiz bir şey!",
                tr = "VKİ'niz **${"%.1f".format(bmi)}** — normal aralığın biraz üzerinde. 🔥 Birlikte çalışabiliriz!",
                ru = "Ваш ИМТ **${"%.1f".format(bmi)}** — немного выше нормы. 🔥 Вместе разберёмся!"
            )
            else -> t(
                en = "Your BMI is **${"%.1f".format(bmi)}** — above recommended range. 💚 Every great journey starts exactly here.",
                az = "BKİ-niz **${"%.1f".format(bmi)}** — tövsiyə edilən aralığın üstündədir. 💚 Hər böyük səyahət buradan başlayır.",
                tr = "VKİ'niz **${"%.1f".format(bmi)}** — önerilen aralığın üzerinde. 💚 Her büyük yolculuk buradan başlar.",
                ru = "Ваш ИМТ **${"%.1f".format(bmi)}** — выше рекомендуемого. 💚 Каждое путешествие начинается здесь."
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
                _state.value = _state.value.copy(profile = profile, step = ChatStep.GOAL, chips = goalChips())
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
                    // Ask for weight direction intent — #8 requirement
                    _state.value = _state.value.copy(
                        profile = profile,
                        step = ChatStep.WEIGHT_DIRECTION,
                        chips = weightDirectionChips(),
                        multiSelect = false
                    )
                    enqueueBot(
                        t(
                            en = "To personalise your roadmap — which direction are you aiming for?",
                            az = "Yol xəritənizi fərdiləşdirmək üçün — hansı istiqamətə yönəlirsiniz?",
                            tr = "Yol haritanızı kişiselleştirmek için — hangi yönü hedefliyorsunuz?",
                            ru = "Для персонализации — в какую сторону вы движетесь?"
                        )
                    )
                } else {
                    // Skip weight direction, go straight to allergies
                    _state.value = _state.value.copy(
                        profile = profile,
                        step = ChatStep.ALLERGIES,
                        chips = allergyChips(),
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
                }
            }

            ChatStep.WEIGHT_DIRECTION -> {
                val direction = when {
                    value.contains("lose") || value.contains("Arıqlamaq") || value.contains("vermek") || value.contains("похудеть", ignoreCase = true) -> "lose"
                    value.contains("gain") || value.contains("qazanmaq") || value.contains("almak") || value.contains("набрать", ignoreCase = true) -> "gain"
                    else -> "maintain"
                }
                val profile = _state.value.profile.copy(weightDirection = direction)
                val bmi = profile.bmi ?: 22f
                val isBmiHealthy = bmi in 18.5f..24.9f

                val directionReply = when (direction) {
                    "lose" -> when {
                        bmi < 18.5f -> t(
                            en = "Your BMI of ${"%.1f".format(bmi)} is already in the underweight range. Losing more weight could be risky. 🙏 I'd suggest focusing on building strength and nourishment instead.",
                            az = "BKİ-niz ${"%.1f".format(bmi)} artıq az çəkili aralıqdadır. Daha çox arıqlamaq riskli ola bilər. 🙏 Bunun əvəzinə güc qurmağa diqqət etməyinizi tövsiyə edirəm.",
                            tr = "VKİ'niz ${"%.1f".format(bmi)} zaten düşük kilolu aralıkta. Daha fazla kilo vermek riskli olabilir. 🙏 Güç inşasına odaklanmanızı öneririm.",
                            ru = "Ваш ИМТ ${"%.1f".format(bmi)} уже в зоне недостаточного веса. Худеть дальше рискованно. 🙏 Предлагаю сосредоточиться на силе и питании."
                        )
                        isBmiHealthy -> t(
                            en = "Your BMI of ${"%.1f".format(bmi)} is in the healthy range. Minor body recomposition is totally achievable — no need for aggressive cuts. ✅",
                            az = "BKİ-niz ${"%.1f".format(bmi)} sağlam aralıqdadır. Kiçik bədən dəyişikliyi tamamilə əldə edilə bilər. ✅",
                            tr = "VKİ'niz ${"%.1f".format(bmi)} sağlıklı aralıkta. Hafif vücut değişimi mükemmel şekilde ulaşılabilir. ✅",
                            ru = "Ваш ИМТ ${"%.1f".format(bmi)} в норме. Лёгкое переформирование тела вполне достижимо. ✅"
                        )
                        else -> t(
                            en = "Based on your BMI of ${"%.1f".format(bmi)}, losing weight is a healthy and achievable goal. 💪 We'll keep it smart — no crash diets.",
                            az = "BKİ-niz ${"%.1f".format(bmi)}-ə görə, arıqlamaq sağlıklı və əldə edilə bilən bir hədəfdir. 💪",
                            tr = "VKİ'niz ${"%.1f".format(bmi)}'e göre kilo vermek sağlıklı bir hedef. 💪 Akıllıca tutacağız.",
                            ru = "При ИМТ ${"%.1f".format(bmi)} похудение — здоровая и достижимая цель. 💪"
                        )
                    }
                    "gain" -> when {
                        bmi > 25f -> t(
                            en = "Your BMI of ${"%.1f".format(bmi)} is above normal. Gaining more weight may not be the healthiest path. 🤔 Consider muscle building through strength training instead.",
                            az = "BKİ-niz ${"%.1f".format(bmi)} normaldan yuxarıdır. Daha çox çəki qazanmaq sağlıklı olmaya bilər. 🤔",
                            tr = "VKİ'niz ${"%.1f".format(bmi)} normalın üzerinde. Daha fazla kilo almak en sağlıklı yol olmayabilir. 🤔",
                            ru = "Ваш ИМТ ${"%.1f".format(bmi)} выше нормы. Набирать ещё вес может быть нездорово. 🤔"
                        )
                        else -> t(
                            en = "With a BMI of ${"%.1f".format(bmi)}, gaining healthy weight through muscle and proper nutrition is absolutely the right move. 💪",
                            az = "BKİ-niz ${"%.1f".format(bmi)} ilə, əzələ və düzgün qidalanma vasitəsilə sağlıklı çəki qazanmaq tamamilə doğru addımdır. 💪",
                            tr = "VKİ'niz ${"%.1f".format(bmi)} ile kas ve uygun beslenme yoluyla sağlıklı kilo almak kesinlikle doğru hamle. 💪",
                            ru = "При ИМТ ${"%.1f".format(bmi)} набор здорового веса через мышцы и питание — абсолютно правильный шаг. 💪"
                        )
                    }
                    else -> t(
                        en = "Maintenance at BMI ${"%.1f".format(bmi)} — smart choice. 📊 We'll focus on consistency and long-term health.",
                        az = "BKİ ${"%.1f".format(bmi)}-də qoruma — ağıllı seçim. 📊",
                        tr = "VKİ ${"%.1f".format(bmi)}'de koruma — akıllıca seçim. 📊",
                        ru = "Поддержание при ИМТ ${"%.1f".format(bmi)} — разумный выбор. 📊"
                    )
                }

                _state.value = _state.value.copy(
                    profile = profile,
                    step = ChatStep.ALLERGIES,
                    chips = allergyChips(),
                    multiSelect = true,
                    selectedItems = emptySet()
                )
                enqueueBot(directionReply)
                delay(300)
                enqueueBot(
                    t(
                        en = "Almost there! Any foods your body can't get along with? Select all that apply 👇",
                        az = "Az qalır! Bədəninizin uyğunlaşa bilmədiyi yeməklər varmı? 👇",
                        tr = "Neredeyse bitti! Vücudunuzun geçinemediği yiyecekler var mı? 👇",
                        ru = "Почти готово! Есть продукты, с которыми организм не ладит? 👇"
                    )
                )
            }

            else -> Unit
        }
    }

    // ── Multi-select with No Restrictions logic (#4) ──────────────────────────

    private fun toggleMulti(value: String) {
        val set = _state.value.selectedItems.toMutableSet()
        val noRestriction = noRestrictionMarker()
        val customKey = customMarker()

        when {
            value == noRestriction -> {
                // #4: selecting "No Restrictions" clears everything else
                if (set.contains(noRestriction)) set.remove(noRestriction)
                else { set.clear(); set.add(noRestriction) }
            }
            else -> {
                // #4: if "No Restrictions" was selected, remove it
                set.remove(noRestriction)
                if (set.contains(value)) set.remove(value) else set.add(value)
            }
        }

        val showCustom = set.contains(customKey)
        _state.value = _state.value.copy(
            selectedItems = set,
            showCustomInput = showCustom,
            requestKeyboard = showCustom   // #3: auto-open keyboard for Custom
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
                    chips = dietaryChips(),
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

    private fun buildFinalAnalysis(profile: SetupProfile): FinalAnalysisUi {
        val currentWeight = profile.weightKg ?: 70f
        val direction = profile.weightDirection
        val targetWeight = when {
            direction == "lose" || profile.goal.contains("Lose") || profile.goal.contains("Arıq") ||
                    profile.goal.contains("Kilo Ver") || profile.goal.contains("Похуд") -> currentWeight - 8f
            direction == "gain" || profile.goal.contains("Muscle") || profile.goal.contains("Əzələ") ||
                    profile.goal.contains("Kas") || profile.goal.contains("мышц") -> currentWeight + 4f
            else -> currentWeight - 2f
        }
        val months = estimateMonths(profile.bmi ?: 24f)
        val dailyCalories = when {
            direction == "lose" -> 1900
            direction == "gain" -> 2500
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

// Helper operator for ChatStep comparison
private operator fun ChatStep.compareTo(other: ChatStep): Int = this.ordinal.compareTo(other.ordinal)