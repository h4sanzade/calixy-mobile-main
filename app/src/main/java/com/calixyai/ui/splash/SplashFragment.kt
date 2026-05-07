package com.calixyai.ui.splash

import android.os.Bundle
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.calixyai.R
import com.calixyai.databinding.FragmentSplashBinding
import com.calixyai.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashFragment : BaseFragment(R.layout.fragment_splash) {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SplashViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSplashBinding.bind(view)

        binding.logoGroup.doOnPreDraw {
            binding.logoGroup.alpha = 0f
            binding.logoGroup.scaleX = 0.86f
            binding.logoGroup.scaleY = 0.86f
            binding.logoGroup.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(700).start()
        }

        viewModel.onIntent(SplashIntent.Load)

        viewLifecycleOwner.lifecycleScope.launch {
            val minDelay = launch { delay((3000L..5000L).random()) }
            val destination = viewModel.state
                .filterNotNull()
                .first { it.destination != null }
                .destination!!
            minDelay.join()

            when (destination) {
                SplashDestination.ONBOARDING ->
                    findNavController().navigate(
                        SplashFragmentDirections.actionSplashFragmentToLanguageSelectFragment()
                    )
                SplashDestination.LOGIN ->
                    findNavController().navigate(
                        SplashFragmentDirections.actionSplashFragmentToLoginFragment()
                    )
                SplashDestination.CHAT_SETUP ->
                    findNavController().navigate(
                        SplashFragmentDirections.actionSplashFragmentToChatSetupFragment()
                    )
                SplashDestination.PAYMENT ->
                    findNavController().navigate(
                        SplashFragmentDirections.actionSplashFragmentToPaymentFragment()
                    )
                SplashDestination.HOME ->
                    findNavController().navigate(
                        SplashFragmentDirections.actionSplashFragmentToHomeFragment()
                    )
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}