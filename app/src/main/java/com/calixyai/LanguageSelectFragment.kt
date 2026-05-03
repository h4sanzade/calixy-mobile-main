package com.calixyai.ui.language

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.calixyai.R
import com.calixyai.databinding.FragmentLanguageSelectBinding
import com.calixyai.ui.common.BaseFragment
import java.util.Locale

class LanguageSelectFragment : BaseFragment(R.layout.fragment_language_select) {

    private var _binding: FragmentLanguageSelectBinding? = null
    private val binding get() = _binding!!

    private var selectedLocale: String = "en"

    private data class LangItem(
        val localeTag: String,
        val rootView: () -> View,
        val radioView: () -> View
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLanguageSelectBinding.bind(view)

        val items = listOf(
            LangItem("az", { binding.itemAz }, { binding.radioAz }),
            LangItem("tr", { binding.itemTr }, { binding.radioTr }),
            LangItem("en", { binding.itemEn }, { binding.radioEn }),
            LangItem("ru", { binding.itemRu }, { binding.radioRu })
        )

        fun applySelection(locale: String) {
            selectedLocale = locale
            items.forEach { item ->
                val isSelected = item.localeTag == locale
                item.rootView().background = requireContext().getDrawable(
                    if (isSelected) R.drawable.bg_lang_item_selected
                    else R.drawable.bg_lang_item
                )
                (item.radioView() as? android.widget.ImageView)?.setImageResource(
                    if (isSelected) R.drawable.ic_radio_selected
                    else R.drawable.ic_radio_unselected
                )
            }
        }

        applySelection("en")

        items.forEach { item ->
            item.rootView().setOnClickListener {
                it.animate().scaleX(0.96f).scaleY(0.96f).setDuration(70)
                    .withEndAction {
                        it.animate().scaleX(1f).scaleY(1f).setDuration(70).start()
                    }.start()
                applySelection(item.localeTag)
            }
        }

        binding.btnContinueLang.setOnClickListener {
            applyLocale(selectedLocale)
            findNavController().navigate(
                LanguageSelectFragmentDirections
                    .actionLanguageSelectFragmentToLoginFragment()
            )
        }
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