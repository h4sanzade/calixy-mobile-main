package com.calixyai.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.calixyai.R
import com.calixyai.databinding.FragmentHomeBinding
import com.calixyai.ui.common.BaseFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : BaseFragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        viewModel.onIntent(HomeIntent.Load)
        viewModel.onIntent(HomeIntent.CheckFirstTimeUser)

        observeState()
        observeEffects()
    }


    private fun observeState() {
        launchAndRepeat {
            viewModel.state.collect { state ->
                binding.tvGreeting.text = state.greeting
                binding.tvName.text = state.name
                binding.tvGoal.text = state.goal
                binding.tvBmi.text = state.bmi
                binding.tvCalories.text = state.calories

                if (state.showDialog) showAICoachDialog()
            }
        }
    }

    // ── Effects ───────────────────────────────────────────────────────────────

    private fun observeEffects() {
        launchAndRepeat {
            viewModel.effects.collect { effect ->
                when (effect) {
                    HomeEffect.NavigateToChatbot -> {
                        findNavController().navigate(
                            HomeFragmentDirections.actionHomeFragmentToChatSetupFragment()
                        )
                    }
                }
            }
        }
    }

    // ── AI Coach Dialog ───────────────────────────────────────────────────────

    private fun showAICoachDialog() {
        if (parentFragmentManager.findFragmentByTag("ai_coach_dialog") != null) return

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Meet Your AI Coach 🤖")
            .setMessage(
                "To provide you with a better experience, you need to share your " +
                        "information with our AI coach."
            )
            .setPositiveButton("Continue") { dialog, _ ->
                dialog.dismiss()
                viewModel.onIntent(HomeIntent.ContinueClicked)
            }
            .setCancelable(false)
            .show()
            .also { it.window?.setBackgroundDrawableResource(android.R.color.transparent) }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}