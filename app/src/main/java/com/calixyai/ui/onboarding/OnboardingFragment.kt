package com.calixyai.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.calixyai.R
import com.calixyai.databinding.FragmentOnboardingBinding
import com.calixyai.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@AndroidEntryPoint
class OnboardingFragment : BaseFragment(R.layout.fragment_onboarding) {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OnboardingViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingBinding.bind(view)

        // LanguageSelectFragment locale-i SharedPreferences-ə yazdı və
        // Activity.attachBaseContext() vasitəsilə context yeniləndi.
        // ViewModel burada yaradılır, buildPages() artıq seçilmiş dili oxuyur.
        viewModel.refreshPages()

        val pages = viewModel.state.value.pages
        binding.viewPager.adapter = OnboardingPagerAdapter(pages)
        binding.indicator.setViewPager(binding.viewPager)

        binding.viewPager.setPageTransformer(CompositePageTransformer().apply {
            addTransformer(MarginPageTransformer(24))
            addTransformer { page, position ->
                page.alpha = 0.3f + (1 - abs(position))
                page.translationX = -position * 80
                val scale = 0.88f + (1 - abs(position)) * 0.12f
                page.scaleX = scale
                page.scaleY = scale
            }
        })

        binding.viewPager.registerOnPageChangeCallback(
            object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    viewModel.onIntent(OnboardingIntent.PageChanged(position))
                }
            }
        )

        binding.btnNext.setOnClickListener { viewModel.onIntent(OnboardingIntent.Next) }
        binding.tvSkip.setOnClickListener { viewModel.onIntent(OnboardingIntent.Skip) }

        launchAndRepeat {
            viewModel.state.collect { state ->
                // Adapter yenilənibsə (refreshPages sonrası) ona tətbiq et
                val currentAdapter = binding.viewPager.adapter as? OnboardingPagerAdapter
                if (currentAdapter?.itemCount != state.pages.size) {
                    binding.viewPager.adapter = OnboardingPagerAdapter(state.pages)
                    binding.indicator.setViewPager(binding.viewPager)
                }

                binding.viewPager.setCurrentItem(state.currentPage, true)
                binding.btnNext.text = if (state.currentPage == state.pages.lastIndex)
                    getString(R.string.get_started)
                else
                    getString(R.string.next)

                if (state.finished) {
                    // DƏYIŞDI: artıq languageSelectFragment deyil, loginFragment-ə keçir
                    findNavController().navigate(
                        OnboardingFragmentDirections
                            .actionOnboardingFragmentToLoginFragment()
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}