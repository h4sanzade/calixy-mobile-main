package com.calixyai.ui.auth.verify

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.calixyai.R
import com.calixyai.databinding.FragmentEmailVerifyBinding
import com.calixyai.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmailVerifyFragment : BaseFragment(R.layout.fragment_email_verify) {

    private var _binding: FragmentEmailVerifyBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EmailVerifyViewModel by viewModels()
    private val args: EmailVerifyFragmentArgs by navArgs()

    private var countDownTimer: CountDownTimer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEmailVerifyBinding.bind(view)

        binding.tvEmail.text = args.email

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigate(
                EmailVerifyFragmentDirections.actionEmailVerifyFragmentToLoginFragment()
            )
        }

        binding.btnResend.setOnClickListener {
            viewModel.onIntent(EmailVerifyIntent.ResendEmail(args.email))
        }

        observeState()
    }

    private fun observeState() {
        launchAndRepeat {
            viewModel.state.collect { state ->
                binding.btnResend.isEnabled = !state.isCooldown && !state.isSending
                binding.btnResend.text = if (state.isSending) "Sending…" else "Resend Verification Email"

                binding.tvStatus.isVisible = state.statusMessage != null
                binding.tvStatus.text = state.statusMessage

                binding.cooldownGroup.isVisible = state.isCooldown

                if (state.isCooldown) {
                    startCountdown(state.cooldownSeconds)
                } else {
                    countDownTimer?.cancel()
                }
            }
        }
    }

    private fun startCountdown(totalSeconds: Int) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(totalSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secs = (millisUntilFinished / 1000).toInt()
                val mins = secs / 60
                val s = secs % 60
                binding.tvCountdown.text = "$mins:${s.toString().padStart(2, '0')}"
            }
            override fun onFinish() {
                viewModel.onIntent(EmailVerifyIntent.CooldownFinished)
            }
        }.start()
    }

    override fun onDestroyView() {
        countDownTimer?.cancel()
        _binding = null
        super.onDestroyView()
    }
}
