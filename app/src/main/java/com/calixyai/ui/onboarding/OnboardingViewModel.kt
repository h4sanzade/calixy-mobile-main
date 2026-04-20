package com.calixyai.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calixyai.R
import com.calixyai.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: AppRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val locale: String
        get() = context.getSharedPreferences("calixy_prefs", Context.MODE_PRIVATE)
            .getString("selected_locale", "en") ?: "en"

    private fun t(en: String, az: String = en, tr: String = en, ru: String = en) =
        when (locale) { "az" -> az; "tr" -> tr; "ru" -> ru; else -> en }

    private fun buildPages() = listOf(
        OnboardingPageUi(
            title = t(
                en = "Your body. Your data. Your rules.",
                az = "Bədəniniz. Məlumatınız. Qaydalarınız.",
                tr = "Vücudunuz. Veriniz. Kurallarınız.",
                ru = "Ваше тело. Ваши данные. Ваши правила."
            ),
            subtitle = t(
                en = "CalixyAI turns your daily habits into a clear, personalized nutrition rhythm.",
                az = "CalixyAI gündəlik vərdişlərinizi aydın, fərdi qidalanma ritminə çevirir.",
                tr = "CalixyAI günlük alışkanlıklarınızı net, kişiselleştirilmiş bir beslenme ritmine dönüştürür.",
                ru = "CalixyAI превращает ваши привычки в чёткий, персональный ритм питания."
            ),
            animationRes = R.raw.onboarding_fitness_1
        ),
        OnboardingPageUi(
            title = t(
                en = "Smart tracking without the noise.",
                az = "Səs-küysüz ağıllı izləmə.",
                tr = "Gürültüsüz akıllı takip.",
                ru = "Умное отслеживание без лишнего шума."
            ),
            subtitle = t(
                en = "Log progress fast, understand calories and macros, and stay focused on what matters.",
                az = "İrəliləyişi tez qeyd edin, kalori və makroları anlayın, vacib olana diqqət yetirin.",
                tr = "İlerlemeyi hızla kaydedin, kalori ve makroları anlayın, önemli olana odaklanın.",
                ru = "Быстро записывайте прогресс, понимайте калории и макросы, фокусируйтесь на важном."
            ),
            animationRes = R.raw.onboarding_fitness_2
        ),
        OnboardingPageUi(
            title = t(
                en = "A premium coach for real life.",
                az = "Real həyat üçün premium məşqçi.",
                tr = "Gerçek yaşam için premium koç.",
                ru = "Премиум-тренер для реальной жизни."
            ),
            subtitle = t(
                en = "Context-aware plans, habit reminders, and beautiful insights that keep you moving.",
                az = "Kontekstdən xəbərdar planlar, vərdiş xatırlatmaları və sizi hərəkətdə saxlayan fikirlər.",
                tr = "Bağlam farkında planlar, alışkanlık hatırlatıcıları ve sizi hareket ettiren içgörüler.",
                ru = "Контекстные планы, напоминания и аналитика, которая держит вас в движении."
            ),
            animationRes = R.raw.onboarding_fitness_3
        )
    )

    private val _state = MutableStateFlow(OnboardingState(pages = buildPages()))
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            OnboardingIntent.Skip -> finish()
            OnboardingIntent.Next -> {
                val current = _state.value.currentPage
                if (current == _state.value.pages.lastIndex) finish()
                else _state.value = _state.value.copy(currentPage = current + 1)
            }
            is OnboardingIntent.PageChanged ->
                _state.value = _state.value.copy(currentPage = intent.page)
        }
    }

    private fun finish() = viewModelScope.launch {
        repository.setOnboardingDone()
        _state.value = _state.value.copy(finished = true)
    }
}