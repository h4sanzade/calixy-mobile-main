package com.calixyai.ui.language

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
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

    private data class LangChip(
        val localeTag: String,
        val flag: String,
        val nativeName: String,
        val englishName: String
    )

    private val languages = listOf(
        LangChip("az", "🇦🇿", "Azərbaycan", "Azerbaijani"),
        LangChip("tr", "🇹🇷", "Türkçe", "Turkish"),
        LangChip("en", "🇬🇧", "English", "English"),
        LangChip("ru", "🇷🇺", "Русский", "Russian")
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLanguageSelectBinding.bind(view)

        setupLanguageChips()

        binding.btnContinueLang.setOnClickListener {
            applyLocale(selectedLocale)
            // Restart activity so all string resources reload in new locale
            requireActivity().recreate()
            // After recreate, SplashViewModel will re-read locale and navigate properly
            // We store "lang_chosen" flag so after recreate we go to splash
            requireContext()
                .getSharedPreferences("calixy_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("lang_chosen", true)
                .apply()
        }
    }

    private fun setupLanguageChips() {
        val chipGroup = binding.chipGroupLanguages
        chipGroup.removeAllViews()

        languages.forEach { lang ->
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = "${lang.flag}  ${lang.nativeName}"
                isCheckable = true
                isChecked = lang.localeTag == selectedLocale
                textSize = 15f
                chipCornerRadius = 16f
                chipMinHeight = 52f
                setPadding(8, 0, 8, 0)

                setTextColor(ContextCompat.getColorStateList(context, R.color.chip_text_selector)
                    ?: android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.text_primary)
                    ))

                updateChipStyle(this, lang.localeTag == selectedLocale)

                setOnClickListener {
                    selectedLocale = lang.localeTag
                    // Update all chips
                    for (i in 0 until chipGroup.childCount) {
                        val c = chipGroup.getChildAt(i) as? com.google.android.material.chip.Chip
                        val isThis = (c == this)
                        c?.isChecked = isThis
                        val tag = c?.tag as? String ?: continue
                        updateChipStyle(c, tag == selectedLocale)
                    }
                }
                tag = lang.localeTag

                // Animate on click
                setOnClickListener {
                    selectedLocale = lang.localeTag
                    animate().scaleX(0.93f).scaleY(0.93f).setDuration(70)
                        .withEndAction {
                            animate().scaleX(1f).scaleY(1f).setDuration(70).start()
                        }.start()
                    for (i in 0 until chipGroup.childCount) {
                        val c = chipGroup.getChildAt(i) as? com.google.android.material.chip.Chip ?: continue
                        val t = c.tag as? String ?: continue
                        c.isChecked = (t == selectedLocale)
                        updateChipStyle(c, t == selectedLocale)
                    }
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun updateChipStyle(chip: com.google.android.material.chip.Chip, selected: Boolean) {
        val bgColor = if (selected)
            android.graphics.Color.parseColor("#E6F9F4")
        else
            android.graphics.Color.parseColor("#EDF6F2")

        val strokeColor = if (selected)
            android.graphics.Color.parseColor("#0DBF85")
        else
            android.graphics.Color.parseColor("#DCF0EA")

        val textColor = if (selected)
            android.graphics.Color.parseColor("#0D1F18")
        else
            android.graphics.Color.parseColor("#5A7A6E")

        chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(bgColor)
        chip.chipStrokeColor = android.content.res.ColorStateList.valueOf(strokeColor)
        chip.chipStrokeWidth = if (selected) 2f else 1f
        chip.setTextColor(textColor)
    }

    private fun applyLocale(localeTag: String) {
        val locale = Locale(localeTag)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        requireContext().resources.updateConfiguration(config, resources.displayMetrics)

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