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

    private fun t(
        en: String,
        az: String = en,
        tr: String = en,
        ru: String = en
    ): String = when (locale) {
        "az" -> az
        "tr" -> tr
        "ru" -> ru
        else -> en
    }

    // Activity chips per locale
    private fun activityChips() = when (locale) {
        "az" -> listOf("🛋️ Divan Sevəni", "🚶 Yüngül Gəzən", "🏃 Orta Aktiv", "🏋️ Zal Daimi", "⚡ Atlet Rejimi", "🧘 Yoqa & Zen")
        "tr" -> listOf("🛋️ Kanepe Tutkunu", "🚶 Hafif Yürüyüşçü", "🏃 Orta Aktif", "🏋️ Spor Salonu Düzenlisi", "⚡ Atlet Modu", "🧘 Yoga & Zen")
        "ru" -> listOf("🛋️ Диванный эксперт", "🚶 Лёгкая ходьба", "🏃 Умеренно активный", "🏋️ Постоянный в зале", "⚡ Режим атлета", "🧘 Йога и дзен")
        else -> listOf("🛋️ Couch Potato", "🚶 Light Walker", "🏃 Moderately Active", "🏋️ Gym Regular", "⚡ Athlete Mode", "🧘 Yoga & Zen")
    }

    // Goal chips per locale
    private fun goalChips() = when (locale) {
        "az" -> listOf("🔥 Arıqla", "💪 Əzələ Qur", "📊 Çəkimi Saxla", "🏃 Formanı Yaxşılaşdır", "❤️ Sadəcə Sağlam Ol")
        "tr" -> listOf("🔥 Kilo Ver", "💪 Kas Yap", "📊 Kilomu Koru", "🏃 Formu Geliştir", "❤️ Sadece Sağlıklı Ol")
        "ru" -> listOf("🔥 Похудеть", "💪 Набрать мышцы", "📊 Сохранить вес", "🏃 Улучшить форму", "❤️ Просто быть здоровым")
        else -> listOf("🔥 Lose Weight", "💪 Build Muscle", "📊 Stay at My Weight", "🏃 Improve Fitness", "❤️ Just Be Healthier")
    }

    // Allergy chips per locale
    private fun allergyChips() = when (locale) {
        "az" -> listOf("🥜 Yer fındığı", "🥛 Süd məhsulları", "🌾 Qluten", "🥚 Yumurta", "🐟 Dəniz məhsulları", "🍯 Soya", "🌰 Ağac qozları", "✏️ Özüm yazım…")
        "tr" -> listOf("🥜 Yer Fıstığı", "🥛 Süt Ürünleri", "🌾 Gluten", "🥚 Yumurta", "🐟 Deniz Ürünleri", "🍯 Soya", "🌰 Ağaç Kuruyemişi", "✏️ Özel…")
        "ru" -> listOf("🥜 Арахис", "🥛 Молочные продукты", "🌾 Глютен", "🥚 Яйца", "🐟 Морепродукты", "🍯 Соя", "🌰 Древесные орехи", "✏️ Своё…")
        else -> listOf("🥜 Peanuts", "🥛 Dairy", "🌾 Gluten", "🥚 Eggs", "🐟 Seafood", "🍯 Soy", "🌰 Tree Nuts", "✏️ Custom…")
    }

    // Dietary chips per locale
    private fun dietaryChips() = when (locale) {
        "az" -> listOf("☪️ Halal", "✡️ Koşer", "🌱 Vegan", "🥗 Vegetarian", "🥩 Keto", "🌾 Paleo", "🚫 Məhdudiyyət Yoxdur")
        "tr" -> listOf("☪️ Helal", "✡️ Koşer", "🌱 Vegan", "🥗 Vejetaryen", "🥩 Keto", "🌾 Paleo", "🚫 Kısıtlama Yok")
        "ru" -> listOf("☪️ Халяль", "✡️ Кошерное", "🌱 Веган", "🥗 Вегетарианец", "🥩 Кето", "🌾 Палео", "🚫 Без ограничений")
        else -> listOf("☪️ Halal", "✡️ Kosher", "🌱 Vegan", "🥗 Vegetarian", "🥩 Keto", "🌾 Paleo", "🚫 No Restrictions")
    }

    // Gender chips per locale
    private fun genderChips() = when (locale) {
        "az" -> listOf("Kişi", "Qadın", "Digər")
        "tr" -> listOf("Erkek", "Kadın", "Diğer")
        "ru" -> listOf("Мужчина", "Женщина", "Другое")
        else -> listOf("Male", "Female", "Other")
    }

    // Custom marker per locale
    private fun customMarker() = when (locale) {
        "az" -> "✏️ Özüm yazım…"
        "tr" -> "✏️ Özel…"
        "ru" -> "✏️ Своё…"
        else -> "✏️ Custom…"
    }

    private fun noRestrictionMarker() = when (locale) {
        "az" -> "🚫 Məhdudiyyət Yoxdur"
        "tr" -> "🚫 Kısıtlama Yok"
        "ru" -> "🚫 Без ограничений"
        else -> "🚫 No Restrictions"
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
        }
    }

    // ── Steps ─────────────────────────────────────────────────────────────────

    private fun initialize() {
        if (_state.value.messages.isNotEmpty()) return
        viewModelScope.launch {
            enqueueBot(
                t(
                    en = "Hey there! I'm Calixy, your personal AI nutrition coach 🤖\n\nBefore we build your plan — what's your **first name**?",
                    az = "Salam! Mən Calixy, şəxsi AI qidalanma məşqçinizəm 🤖\n\nPlanınızı qurmadan əvvəl — **adınız** nədir?",
                    tr = "Merhaba! Ben Calixy, kişisel AI beslenme koçunuzum 🤖\n\nPlanınızı oluşturmadan önce — **adınız** nedir?",
                    ru = "Привет! Я Calixy, ваш персональный AI-нутрициолог 🤖\n\nПрежде чем составить план — как вас **зовут**?"
                )
            )
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
                enqueueBot(
                    t(
                        en = "Love the name **$name**! 🎉 Now, what's your surname?",
                        az = "Gözəl addir **$name**! 🎉 İndi, soyadınız nədir?",
                        tr = "Harika bir isim **$name**! 🎉 Şimdi, soyadınız nedir?",
                        ru = "Отличное имя **$name**! 🎉 Теперь — какая у вас **фамилия**?"
                    )
                )
            }

            ChatStep.LAST_NAME -> {
                val last = value.trim()
                appendUser(last)
                val profile = _state.value.profile.copy(lastName = last)
                val full = "${profile.firstName} $last"
                _state.value = _state.value.copy(profile = profile, step = ChatStep.GENDER)
                enqueueBot(
                    t(
                        en = "**$full** — that's a name that means business. Let's keep the momentum going! 💪",
                        az = "**$full** — bu ciddi bir addır. İrəliyə davam edək! 💪",
                        tr = "**$full** — bu ciddi bir isim. Devam edelim! 💪",
                        ru = "**$full** — серьёзное имя. Продолжаем! 💪"
                    )
                )
                delay(400)
                enqueueBot(
                    t(
                        en = "Alright ${profile.firstName}, quick question — how do you identify? This helps me personalize your plan perfectly.",
                        az = "${profile.firstName}, sürətli sual — özünüzü necə təyin edirsiniz? Bu planı mükəmməl fərdiləşdirməyə kömək edir.",
                        tr = "${profile.firstName}, hızlı bir soru — kendinizi nasıl tanımlıyorsunuz? Bu planınızı mükemmel kişiselleştirmeme yardımcı olur.",
                        ru = "${profile.firstName}, быстрый вопрос — как вы себя идентифицируете? Это поможет мне идеально персонализировать план."
                    )
                )
                _state.value = _state.value.copy(
                    showInput = false,
                    chips = genderChips(),
                    multiSelect = false
                )
            }

            ChatStep.AGE -> {
                val age = value.filter { it.isDigit() }.toIntOrNull() ?: 0
                appendUser("$age")
                val profile = _state.value.profile.copy(age = age)
                _state.value = _state.value.copy(profile = profile, step = ChatStep.HEIGHT_WEIGHT)

                val ageReply = when {
                    age in 13..25 -> t(
                        en = "**$age** — peak energy years! ⚡ Your metabolism is your superpower right now. Let's use it well.",
                        az = "**$age** — pik enerji illəri! ⚡ Metabolizmanız indi sizin super gücünüzdür. Ondan yaxşı istifadə edək.",
                        tr = "**$age** — zirve enerji yılları! ⚡ Metabolizmanız şu an süper gücünüz. Onu iyi kullanalım.",
                        ru = "**$age** — пик энергии! ⚡ Ваш метаболизм сейчас — ваша суперсила. Используем его на полную."
                    )
                    age in 26..45 -> t(
                        en = "**$age**! That's the sweet spot where discipline meets experience. 🔥 You know what you want — we'll help you get there faster.",
                        az = "**$age**! Bu intizamın təcrübəylə buluşduğu yerdir. 🔥 Nə istədiyinizi bilirsiniz — daha sürətli çatmağa kömək edəcəyik.",
                        tr = "**$age**! Disiplinin deneyimle buluştuğu nokta. 🔥 Ne istediğinizi biliyorsunuz — daha hızlı ulaşmanıza yardımcı olacağız.",
                        ru = "**$age**! Это точка, где дисциплина встречается с опытом. 🔥 Вы знаете, чего хотите — поможем достичь быстрее."
                    )
                    else -> t(
                        en = "**$age** — and clearly taking health seriously. 💪 Smart move. At this stage, quality nutrition makes an enormous difference.",
                        az = "**$age** — sağlamlığa ciddi yanaşırsınız. 💪 Ağıllı addım. Bu mərhələdə keyfiyyətli qidalanma böyük fərq yaradır.",
                        tr = "**$age** — sağlığa ciddi yaklaşıyorsunuz. 💪 Akıllıca. Bu aşamada kaliteli beslenme büyük fark yaratır.",
                        ru = "**$age** — явно серьёзно относитесь к здоровью. 💪 Правильный подход. На этом этапе питание играет огромную роль."
                    )
                }
                enqueueBot(ageReply)
                delay(300)
                enqueueBot(
                    t(
                        en = "Now let's get your body measurements. Use the sliders below — drag to your exact height and weight. 📏",
                        az = "İndi bədən ölçülərinizi götürək. Aşağıdakı sürgülərdən istifadə edin — boy və çəkinizi dəqiq seçin. 📏",
                        tr = "Şimdi vücut ölçülerinizi alalım. Aşağıdaki kaydırıcıları kullanın — boy ve kilonuzu tam olarak seçin. 📏",
                        ru = "Теперь замерим ваши параметры. Используйте слайдеры ниже — выберите точный рост и вес. 📏"
                    )
                )
                _state.value = _state.value.copy(showInput = false, showSliders = true, chips = emptyList())
            }

            else -> Unit
        }
    }

    fun confirmSliders(heightCm: Int, weightKg: Float) = viewModelScope.launch {
        val displayWeight = if (weightKg == weightKg.toInt().toFloat())
            "${weightKg.toInt()} ${t("kg", "kq", "kg", "кг")}"
        else
            "${"%.1f".format(weightKg)} ${t("kg", "kq", "kg", "кг")}"

        appendUser(
            t(
                en = "Height: **$heightCm cm** · Weight: **$displayWeight**",
                az = "Boy: **$heightCm sm** · Çəki: **$displayWeight**",
                tr = "Boy: **$heightCm cm** · Ağırlık: **$displayWeight**",
                ru = "Рост: **$heightCm см** · Вес: **$displayWeight**"
            )
        )

        val bmi = calculateBmi(heightCm, weightKg)
        val estimatedMonths = estimateMonths(bmi)

        val bmiCategory = when {
            bmi < 18.5f -> "underweight"
            bmi < 25f -> "normal"
            bmi < 30f -> "overweight"
            else -> "obese"
        }

        val verdict = when (bmiCategory) {
            "underweight" -> t(
                en = "Your BMI is **${"%.1f".format(bmi)}** — you're in the underweight zone. No worries at all! 🌱 We'll focus on building strong nutritional foundations and helping you gain in the healthiest way possible.",
                az = "BKİ-niz **${"%.1f".format(bmi)}** — az çəkili zonadasınız. Heç narahat olmayın! 🌱 Güclü qidalanma əsasları qurmağa diqqət edəcəyik.",
                tr = "VKİ'niz **${"%.1f".format(bmi)}** — zayıf bölgedesiniz. Hiç merak etmeyin! 🌱 Güçlü beslenme temelleri oluşturmaya odaklanacağız.",
                ru = "Ваш ИМТ **${"%.1f".format(bmi)}** — вы в зоне недостаточного веса. Не переживайте! 🌱 Сосредоточимся на построении правильного питания."
            )
            "normal" -> t(
                en = "Your BMI is **${"%.1f".format(bmi)}** — you're right in the healthy range. Excellent! ✅ Now we'll focus on dialing in your body composition, energy levels, and long-term consistency.",
                az = "BKİ-niz **${"%.1f".format(bmi)}** — sağlam aralıqdasınız. Əla! ✅ İndi bədən kompozisiyasına, enerji səviyyəsinə və uzunmüddətli ardıcıllığa diqqət edəcəyik.",
                tr = "VKİ'niz **${"%.1f".format(bmi)}** — sağlıklı aralıktasınız. Mükemmel! ✅ Şimdi vücut kompozisyonuna, enerji seviyesine ve uzun vadeli tutarlılığa odaklanacağız.",
                ru = "Ваш ИМТ **${"%.1f".format(bmi)}** — вы в здоровой норме. Отлично! ✅ Теперь сосредоточимся на составе тела, уровне энергии и долгосрочной последовательности."
            )
            "overweight" -> t(
                en = "Your BMI is **${"%.1f".format(bmi)}** — slightly above the normal range. Nothing we can't fix together! 🔥 People in your exact range typically reach their target within 3–4 months.",
                az = "BKİ-niz **${"%.1f".format(bmi)}** — normal aralığın bir az üstündədir. Birlikdə düzəldə biləcəyimiz bir şey! 🔥 Sizin aralığınızdakı insanlar adətən 3-4 ay içində hədəfə çatırlar.",
                tr = "VKİ'niz **${"%.1f".format(bmi)}** — normal aralığın biraz üzerinde. Birlikte düzeltemeyeceğimiz bir şey yok! 🔥 Sizin aralığınızdaki kişiler genellikle 3-4 ay içinde hedefe ulaşıyor.",
                ru = "Ваш ИМТ **${"%.1f".format(bmi)}** — немного выше нормы. Это поправимо! 🔥 Люди с таким ИМТ обычно достигают цели за 3–4 месяца."
            )
            else -> t(
                en = "Your BMI is **${"%.1f".format(bmi)}** — above the recommended range right now. But every great journey starts exactly where you are. 💚 Step by step, we'll get you there.",
                az = "BKİ-niz **${"%.1f".format(bmi)}** — hazırda tövsiyə edilən aralığın üstündədir. Lakin hər böyük səyahət tam burada başlayır. 💚 Addım-addım, ora çatacağıq.",
                tr = "VKİ'niz **${"%.1f".format(bmi)}** — şu an önerilen aralığın üzerinde. Ama her büyük yolculuk tam burada başlar. 💚 Adım adım oraya ulaşacağız.",
                ru = "Ваш ИМТ **${"%.1f".format(bmi)}** — выше рекомендуемого. Но каждое большое путешествие начинается именно с того места, где вы находитесь. 💚 Шаг за шагом — дойдём."
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
                en = "How would you describe your typical day? Be real with me — I'm not here to judge 😄",
                az = "Tipik gününüzü necə təsvir edərdiniz? Mənə açıq olun — mən mühakimə etmirəm 😄",
                tr = "Tipik gününüzü nasıl tanımlarsınız? Bana gerçekçi olun — burada yargılamak için değilim 😄",
                ru = "Как бы вы описали свой обычный день? Будьте честны — я не осуждаю 😄"
            )
        )
        _state.value = _state.value.copy(
            showInput = false,
            chips = activityChips()
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
                val response = when {
                    value == "Male" || value == "Erkek" || value == "Kişi" || value == "Мужчина" -> t(
                        en = "A man with a plan. I respect that. 🫡 Let's build something powerful together.",
                        az = "Planı olan bir kişi. Hörmətlə qəbul edirəm. 🫡 Birlikdə güclü bir şey quracağıq.",
                        tr = "Planı olan bir adam. Bunu saygıyla karşılıyorum. 🫡 Birlikte güçlü bir şey inşa edelim.",
                        ru = "Мужчина с планом. Уважаю. 🫡 Вместе построим что-то мощное."
                    )
                    value == "Female" || value == "Kadın" || value == "Qadın" || value == "Женщина" -> t(
                        en = "Strong, determined, and ready to level up. ✨ I love that energy. Let's do this.",
                        az = "Güclü, qətiyyətli və irəliləməyə hazır. ✨ Bu enerji xoşuma gəlir. Başlayaq.",
                        tr = "Güçlü, kararlı ve seviye atlamaya hazır. ✨ Bu enerjiyi seviyorum. Hadi başlayalım.",
                        ru = "Сильная, решительная и готовая к росту. ✨ Обожаю такую энергию. Поехали."
                    )
                    else -> t(
                        en = "Individuality is strength. 🌈 Your uniqueness is exactly what makes this plan special.",
                        az = "Fərdiyyət gücdür. 🌈 Sizin unikallığınız məhz bu planı xüsusi edir.",
                        tr = "Bireysellik güçtür. 🌈 Benzersizliğiniz bu planı özel yapan şeydir.",
                        ru = "Индивидуальность — это сила. 🌈 Ваша уникальность — именно то, что делает этот план особенным."
                    )
                }
                enqueueBot(response)
                delay(300)
                enqueueBot(
                    t(
                        en = "How old are you? Age helps me calibrate your plan precisely. (Don't worry — it stays between us 🤫)",
                        az = "Neçə yaşınız var? Yaş planı dəqiq kalibr etməyə kömək edir. (Narahat olmayın — aramızda qalır 🤫)",
                        tr = "Kaç yaşındasınız? Yaş, planınızı hassas kalibre etmeme yardımcı olur. (Merak etmeyin — aramızda kalır 🤫)",
                        ru = "Сколько вам лет? Возраст помогает точно настроить план. (Не беспокойтесь — это останется между нами 🤫)"
                    )
                )
            }

            ChatStep.ACTIVITY -> {
                val profile = _state.value.profile.copy(activityLevel = value)
                val activityReply = when {
                    value.contains("Couch") || value.contains("Divan") || value.contains("Kanepe") || value.contains("Диванный") -> t(
                        en = "Honest answer — I love it! 😄 Starting from zero actually means the fastest early gains.",
                        az = "Dürüst cavab — xoşuma gəlir! 😄 Sıfırdan başlamaq əslində ən sürətli ilkin nəticə deməkdir.",
                        tr = "Dürüst cevap — bunu seviyorum! 😄 Sıfırdan başlamak aslında en hızlı erken kazanımlar anlamına gelir.",
                        ru = "Честный ответ — обожаю! 😄 Начать с нуля — значит самые быстрые ранние результаты."
                    )
                    value.contains("Light") || value.contains("Yüngül") || value.contains("Hafif") || value.contains("Лёгкая") -> t(
                        en = "A light walker — that's a great foundation. 🚶 Consistency beats intensity every time.",
                        az = "Yüngül gəzən — bu əla bir əsasdır. 🚶 Ardıcıllıq hər zaman intensivliyi üstələyir.",
                        tr = "Hafif yürüyüşçü — bu harika bir temel. 🚶 Tutarlılık her zaman yoğunluğu yener.",
                        ru = "Лёгкая ходьба — отличная база. 🚶 Последовательность всегда побеждает интенсивность."
                    )
                    value.contains("Moderate") || value.contains("Orta") || value.contains("Умеренно") -> t(
                        en = "Moderately active — solid base to build from! 🏃 You've already got the habit.",
                        az = "Orta aktiv — güclü bir əsas! 🏃 Vərdişiniz artıq var.",
                        tr = "Orta aktif — üzerine inşa edilecek sağlam bir temel! 🏃 Alışkanlığınız zaten var.",
                        ru = "Умеренная активность — крепкая база! 🏃 Привычка уже есть."
                    )
                    value.contains("Gym") || value.contains("Zal") || value.contains("Spor") || value.contains("Постоянный") -> t(
                        en = "Gym regular — now we're talking! 🏋️ We'll make sure your nutrition matches your effort.",
                        az = "Zal daimi — indi danışırıq! 🏋️ Qidalanmanızın cəhdinizlə uyğun olduğundan əmin olacağıq.",
                        tr = "Spor salonu düzenlisi — şimdi konuşuyoruz! 🏋️ Beslenmenizin çabalarınızla eşleşmesini sağlayacağız.",
                        ru = "Постоянный в зале — вот теперь разговор! 🏋️ Убедимся, что питание соответствует вашим усилиям."
                    )
                    value.contains("Athlete") || value.contains("Atlet") || value.contains("атлета") -> t(
                        en = "Athlete mode! ⚡ Performance nutrition is a whole different game — and Calixy is built for exactly that.",
                        az = "Atlet rejimi! ⚡ Performans qidalanması tamam başqa oyundur — Calixy məhz bunun üçün qurulub.",
                        tr = "Atlet modu! ⚡ Performans beslenmesi bambaşka bir oyun — Calixy tam da bunun için tasarlandı.",
                        ru = "Режим атлета! ⚡ Спортивное питание — это совсем другая игра, и Calixy создан именно для этого."
                    )
                    else -> t(
                        en = "Yoga & Zen — beautiful choice. 🧘 Mindful movement deserves mindful nutrition.",
                        az = "Yoqa & Zen — gözəl seçim. 🧘 Şüurlu hərəkət şüurlu qidalanmaya layiqdir.",
                        tr = "Yoga & Zen — güzel seçim. 🧘 Bilinçli hareket bilinçli beslenmeyi hak eder.",
                        ru = "Йога и дзен — прекрасный выбор. 🧘 Осознанное движение заслуживает осознанного питания."
                    )
                }
                _state.value = _state.value.copy(
                    profile = profile,
                    step = ChatStep.GOAL,
                    chips = goalChips()
                )
                enqueueBot(activityReply)
                delay(300)
                enqueueBot(
                    t(
                        en = "Now for the big question — what's the main mission? Pick your primary goal:",
                        az = "İndi böyük sual — əsas missiya nədir? Əsas hədəfinizi seçin:",
                        tr = "Şimdi büyük soru — ana misyon nedir? Birincil hedefinizi seçin:",
                        ru = "Теперь главный вопрос — в чём главная миссия? Выберите основную цель:"
                    )
                )
            }

            ChatStep.GOAL -> {
                val profile = _state.value.profile.copy(goal = value)
                val goalReply = when {
                    value.contains("Lose") || value.contains("Arıq") || value.contains("Kilo") || value.contains("Похуд") -> t(
                        en = "Locked in. 🔥 Fat loss done right — sustainable, smart, and without starving yourself. That's the Calixy way.",
                        az = "Qəbul edildi. 🔥 Düzgün edilmiş yağ itkisi — davamlı, ağıllı və ac qalmadan. Bu Calixy üsludur.",
                        tr = "Kilitlendi. 🔥 Doğru yapılan yağ kaybı — sürdürülebilir, akıllı ve aç kalmadan. Calixy yolu bu.",
                        ru = "Принято. 🔥 Жиросжигание правильно — устойчиво, умно и без голодания. Это путь Calixy."
                    )
                    value.contains("Muscle") || value.contains("Əzələ") || value.contains("Kas") || value.contains("мышц") -> t(
                        en = "Building muscle it is! 💪 Gains take the right calories and the right timing. We'll nail both.",
                        az = "Əzələ qurmaq! 💪 Nəticələr doğru kalorilər və doğru vaxtlama tələb edir. İkisini də düzgün edəcəyik.",
                        tr = "Kas yapmak! 💪 Kazanımlar doğru kalori ve doğru zamanlamayı gerektirir. İkisini de halledelim.",
                        ru = "Набор мышц! 💪 Для роста нужны правильные калории и правильное время. Разберёмся с обоим."
                    )
                    value.contains("Stay") || value.contains("Çəkimi") || value.contains("Kilomu") || value.contains("Сохран") -> t(
                        en = "Maintenance is seriously underrated. 📊 Keeping a healthy weight long-term takes real strategy.",
                        az = "Qoruma ciddi şəkildə qiymətləndirilmir. 📊 Uzunmüddətli sağlam çəkini saxlamaq həqiqi strategiya tələb edir.",
                        tr = "Koruma ciddi şekilde küçümseniyor. 📊 Uzun vadede sağlıklı kiloyu korumak gerçek bir strateji gerektirir.",
                        ru = "Поддержание веса серьёзно недооценивается. 📊 Долгосрочное удержание здорового веса требует реальной стратегии."
                    )
                    else -> t(
                        en = "A goal that changes everything. 🏃 Energy, sleep, mood — all of it improves. Let's go!",
                        az = "Hər şeyi dəyişdirən bir hədəf. 🏃 Enerji, yuxu, əhval — hamısı yaxşılaşır. Gedək!",
                        tr = "Her şeyi değiştiren bir hedef. 🏃 Enerji, uyku, ruh hali — hepsi iyileşir. Hadi gidelim!",
                        ru = "Цель, меняющая всё. 🏃 Энергия, сон, настроение — всё улучшается. Поехали!"
                    )
                }
                _state.value = _state.value.copy(
                    profile = profile,
                    step = ChatStep.ALLERGIES,
                    chips = allergyChips(),
                    multiSelect = true,
                    selectedItems = emptySet()
                )
                enqueueBot(goalReply)
                delay(300)
                enqueueBot(
                    t(
                        en = "Almost there! Are there any foods your body just can't get along with? Select all that apply — or skip if you're allergy-free 👇",
                        az = "Az qalır! Bədəninizin uyğunlaşa bilmədiyi yeməklər varmı? Hamısını seçin — allergiya yoxdursa keçin 👇",
                        tr = "Neredeyse bitti! Vücudunuzun geçinemediği yiyecekler var mı? Tümünü seçin — alerjiniz yoksa atlayın 👇",
                        ru = "Почти готово! Есть ли продукты, с которыми ваш организм не ладит? Выберите все подходящие — или пропустите, если аллергий нет 👇"
                    )
                )
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
        appendUser(currentSelected.joinToString(" · ").ifBlank {
            t("None", "Heç biri", "Hiçbiri", "Нет")
        })

        when (_state.value.step) {
            ChatStep.ALLERGIES -> {
                val allergyReply = when {
                    currentSelected.isEmpty() -> t(
                        en = "No allergies — lucky you! 🍀 That gives us maximum flexibility to design a truly varied and delicious plan.",
                        az = "Allergiya yoxdur — şanslısınız! 🍀 Bu bizə həqiqətən müxtəlif və dadlı plan hazırlamaq üçün maksimum çeviklik verir.",
                        tr = "Alerji yok — şanslısınız! 🍀 Bu bize gerçekten çeşitli ve lezzetli bir plan tasarlamak için maksimum esneklik veriyor.",
                        ru = "Нет аллергий — вам повезло! 🍀 Это даёт нам максимальную гибкость для разнообразного и вкусного плана."
                    )
                    else -> t(
                        en = "Got it — ${currentSelected.joinToString(", ")} are off the table. Don't worry, there's still a world of amazing food waiting for you. 🌍",
                        az = "Qəbul edildi — ${currentSelected.joinToString(", ")} istisna edildi. Narahat olmayın, hələ sizin üçün gözəl yemək dünyası gözləyir. 🌍",
                        tr = "Tamam — ${currentSelected.joinToString(", ")} devre dışı. Merak etmeyin, sizin için harika yiyeceklerle dolu bir dünya hâlâ bekliyor. 🌍",
                        ru = "Понял — ${currentSelected.joinToString(", ")} исключены. Не переживайте, вас ждёт целый мир потрясающей еды. 🌍"
                    )
                }
                _state.value = _state.value.copy(
                    profile = _state.value.profile.copy(
                        allergies = currentSelected, customAllergy = customValue
                    ),
                    step = ChatStep.DIETARY,
                    chips = dietaryChips(),
                    selectedItems = emptySet(),
                    multiSelect = true
                )
                enqueueBot(allergyReply)
                delay(300)
                enqueueBot(
                    t(
                        en = "Last question, I promise! 🙏 Any dietary rules or lifestyles I should know about?",
                        az = "Son sual, söz verirəm! 🙏 Bilməli olduğum hər hansı pəhriz qaydaları və ya həyat tərzi varmı?",
                        tr = "Son soru, söz veriyorum! 🙏 Bilmem gereken herhangi bir diyet kuralı veya yaşam tarzı var mı?",
                        ru = "Последний вопрос, обещаю! 🙏 Есть ли какие-либо диетические правила или образ жизни, о которых мне стоит знать?"
                    )
                )
            }

            ChatStep.DIETARY -> {
                val profile = _state.value.profile.copy(dietaryRules = currentSelected)
                val hasNoRestriction = currentSelected.any { it.contains("No Restrictions") || it.contains("Məhdudiyyət") || it.contains("Kısıtlama") || it.contains("Без ограничений") }
                val isVegan = currentSelected.any { it.contains("Vegan") || it.contains("Веган") }
                val isKeto = currentSelected.any { it.contains("Keto") || it.contains("Кето") }

                val dietaryReply = when {
                    hasNoRestriction || currentSelected.isEmpty() -> t(
                        en = "No dietary restrictions — excellent! 🎯 That gives us the widest possible toolkit to build you an incredible, varied plan.",
                        az = "Pəhriz məhdudiyyəti yoxdur — əla! 🎯 Bu bizə inanılmaz, müxtəlif plan qurmaq üçün ən geniş toolkit verir.",
                        tr = "Diyet kısıtlaması yok — mükemmel! 🎯 Bu bize inanılmaz, çeşitli bir plan oluşturmak için en geniş araç setini veriyor.",
                        ru = "Никаких диетических ограничений — отлично! 🎯 Это даёт нам широчайший инструментарий для создания разнообразного плана."
                    )
                    isVegan -> t(
                        en = "Vegan lifestyle — respect! 🌿 Plant-based nutrition done right is genuinely powerful.",
                        az = "Vegan həyat tərzi — hörmətlə! 🌿 Düzgün edilmiş bitki əsaslı qidalanma həqiqətən güclüdür.",
                        tr = "Vegan yaşam tarzı — saygı! 🌿 Doğru yapılan bitki bazlı beslenme gerçekten güçlüdür.",
                        ru = "Веганский образ жизни — уважаю! 🌿 Правильное растительное питание действительно мощное."
                    )
                    isKeto -> t(
                        en = "Keto! ⚡ High fat, low carb — when done correctly, it's remarkable for fat burning.",
                        az = "Keto! ⚡ Yüksək yağ, az karbohidrat — düzgün ediləndə yağ yandırmaq üçün əla.",
                        tr = "Keto! ⚡ Yüksek yağ, düşük karbonhidrat — doğru yapıldığında yağ yakımı için mükemmel.",
                        ru = "Кето! ⚡ Высокий жир, мало углеводов — при правильном выполнении отлично для жиросжигания."
                    )
                    else -> t(
                        en = "Perfect — ${currentSelected.joinToString(", ")} noted. Your plan will respect every one of these preferences.",
                        az = "Mükəmməl — ${currentSelected.joinToString(", ")} qeyd edildi. Planınız bu üstünlüklərin hər birinə hörmət edəcək.",
                        tr = "Mükemmel — ${currentSelected.joinToString(", ")} not alındı. Planınız bu tercihlerin her birine saygı gösterecek.",
                        ru = "Отлично — ${currentSelected.joinToString(", ")} отмечено. Ваш план учтёт каждое из этих предпочтений."
                    )
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
                enqueueBot(
                    t(
                        en = "${profile.firstName}, I've crunched every number, cross-referenced your profile, and built your complete picture. 📊 Here it is:",
                        az = "${profile.firstName}, hər rəqəmi hesabladım, profilinizi çarpaz yoxladım və tam tablonuzu hazırladım. 📊 Budur:",
                        tr = "${profile.firstName}, her sayıyı hesapladım, profilinizi çapraz kontrol ettim ve tam tablonuzu oluşturdum. 📊 İşte:",
                        ru = "${profile.firstName}, я проанализировал каждую цифру, сверился с вашим профилем и составил полную картину. 📊 Вот она:"
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
                        en = "This is your personalized CalixyAI roadmap — built around **you**, not a template. Ready to unlock the full plan? 🚀",
                        az = "Bu sizin fərdi CalixyAI yol xəritənizdir — **sizin** ətrafında qurulub, şablon deyil. Tam planı açmağa hazırsınız? 🚀",
                        tr = "Bu sizin kişiselleştirilmiş CalixyAI yol haritanız — **siz** etrafında oluşturuldu, şablon değil. Tam planı açmaya hazır mısınız? 🚀",
                        ru = "Это ваша персональная дорожная карта CalixyAI — построена вокруг **вас**, а не по шаблону. Готовы разблокировать полный план? 🚀"
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
        val targetWeight = when {
            profile.goal.contains("Lose") || profile.goal.contains("Arıq") || profile.goal.contains("Kilo Ver") || profile.goal.contains("Похуд") -> currentWeight - 8f
            profile.goal.contains("Muscle") || profile.goal.contains("Əzələ") || profile.goal.contains("Kas") || profile.goal.contains("мышц") -> currentWeight + 4f
            else -> currentWeight - 2f
        }
        val months = estimateMonths(profile.bmi ?: 24f)
        val dailyCalories = when {
            profile.goal.contains("Lose") || profile.goal.contains("Arıq") || profile.goal.contains("Kilo Ver") || profile.goal.contains("Похуд") -> 1900
            profile.goal.contains("Muscle") || profile.goal.contains("Əzələ") || profile.goal.contains("Kas") || profile.goal.contains("мышц") -> 2500
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