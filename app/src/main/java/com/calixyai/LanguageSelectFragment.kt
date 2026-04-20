package com.calixyai.ui.language

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.calixyai.R
import com.calixyai.databinding.FragmentLanguageSelectBinding
import com.calixyai.ui.common.BaseFragment
import java.util.Locale

class LanguageSelectFragment : BaseFragment(R.layout.fragment_language_select) {

    private var _binding: FragmentLanguageSelectBinding? = null
    private val binding get() = _binding!!

    private var selectedLocale: String = "en"

    // Map: card view id → (locale tag, checkmark view id)
    private data class LangItem(val localeTag: String, val card: LinearLayout, val check: ImageView)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLanguageSelectBinding.bind(view)

        val items = listOf(
            LangItem("az", binding.cardAz, binding.checkAz),
            LangItem("tr", binding.cardTr, binding.checkTr),
            LangItem("en", binding.cardEn, binding.checkEn),
            LangItem("ru", binding.cardRu, binding.checkRu)
        )

        // Default select English
        selectLang("en", items)

        items.forEach { item ->
            item.card.setOnClickListener {
                animateCard(it)
                selectLang(item.localeTag, items)
            }
        }

        binding.btnContinueLang.setOnClickListener {
            applyLocale(selectedLocale)
            findNavController().navigate(
                LanguageSelectFragmentDirections.actionLanguageSelectFragmentToOnboardingFragment()
            )
        }
    }

    private fun selectLang(locale: String, items: List<LangItem>) {
        selectedLocale = locale
        items.forEach { item ->
            val isSelected = item.localeTag == locale
            item.check.visibility = if (isSelected) View.VISIBLE else View.GONE

            val bgDrawable = if (isSelected)
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_card_elevated)
            else
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_card)
            item.card.background = bgDrawable
        }
    }

    private fun animateCard(v: View) {
        v.animate()
            .scaleX(0.96f).scaleY(0.96f)
            .setDuration(80)
            .withEndAction {
                v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
            }.start()
    }

    private fun applyLocale(localeTag: String) {
        val locale = Locale(localeTag)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        requireContext().resources.updateConfiguration(config, resources.displayMetrics)

        // Persist selection
        requireContext()
            .getSharedPreferences("calixy_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("selected_locale", localeTag)
            .apply()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}