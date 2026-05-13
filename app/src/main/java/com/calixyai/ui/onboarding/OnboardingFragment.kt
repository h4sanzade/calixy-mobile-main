package com.calixyai.ui.onboarding

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.calixyai.R
import com.calixyai.databinding.FragmentOnboardingBinding
import com.calixyai.ui.common.BaseFragment

data class OnboardingPage(
    val tag: String,
    val title: String,
    val subtitle: String,
    val pageIndex: Int
)

class OnboardingFragment : BaseFragment(R.layout.fragment_onboarding) {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!
    private val dotViews = mutableListOf<View>()

    companion object {
        private val PAGES = listOf(
            OnboardingPage(
                tag      = "Personalized",
                title    = "Your body.\nYour data.\nYour rules.",
                subtitle = "CalixyAI turns your daily habits into a clear, personalized nutrition rhythm built only for you.",
                pageIndex = 0
            ),
            OnboardingPage(
                tag      = "Smart Tracking",
                title    = "Smart tracking\nwithout the noise.",
                subtitle = "Log meals in seconds. Understand calories and macros instantly. Stay focused on what actually matters.",
                pageIndex = 1
            ),
            OnboardingPage(
                tag      = "Step 1 of 3",
                title    = "Point. Shoot.\nLet Calixy think.",
                subtitle = "Frame your meal and tap capture. Your AI coach analyzes it in seconds.",
                pageIndex = 2
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingBinding.bind(view)

        setupViewPager()
        setupDots()
        setupClickListeners()
    }

    private fun setupViewPager() {
        binding.viewPager.apply {
            adapter = OnboardingPagerAdapter(PAGES)
            offscreenPageLimit = 2
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateDots(position)
                    updateNextButtonText(position)
                }
            })
        }
    }

    private fun setupDots() {
        val container = binding.dotsContainer
        container.removeAllViews()
        dotViews.clear()
        PAGES.forEachIndexed { index, _ ->
            val dot = createDotView(index == 0)
            container.addView(dot)
            dotViews.add(dot)
        }
    }

    private fun createDotView(isActive: Boolean): View {
        return View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                if (isActive) 22.dpToPx() else 6.dpToPx(),
                6.dpToPx()
            ).apply { marginEnd = 6.dpToPx() }
            background = ContextCompat.getDrawable(
                requireContext(),
                if (isActive) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive
            )
        }
    }

    private fun updateDots(selectedPosition: Int) {
        dotViews.forEachIndexed { index, dot ->
            val isActive = index == selectedPosition
            val lp = dot.layoutParams as LinearLayout.LayoutParams
            lp.width = if (isActive) 22.dpToPx() else 6.dpToPx()
            dot.layoutParams = lp
            dot.background = ContextCompat.getDrawable(
                requireContext(),
                if (isActive) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive
            )
            dot.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
        }
    }

    private fun updateNextButtonText(position: Int) {
        binding.btnNext.text = if (position == PAGES.size - 1)
            getString(R.string.get_started)
        else
            getString(R.string.next)
    }

    private fun setupClickListeners() {
        binding.tvSkip.setOnClickListener { navigateToLogin() }
        binding.btnNext.setOnClickListener { handleNextClick() }
    }

    private fun handleNextClick() {
        val currentPosition = binding.viewPager.currentItem
        if (currentPosition < PAGES.size - 1) {
            binding.viewPager.setCurrentItem(currentPosition + 1, true)
        } else {
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        findNavController().navigate(
            OnboardingFragmentDirections.actionOnboardingFragmentToLoginFragment()
        )
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}