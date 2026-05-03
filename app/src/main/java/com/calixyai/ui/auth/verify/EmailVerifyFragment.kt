package com.calixyai.ui.auth.verify

import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
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

        setupOtpBoxes()

        binding.btnVerify.setOnClickListener {
            val code = collectOtpCode()
            viewModel.onIntent(EmailVerifyIntent.VerifyCode(args.email, code))
        }

        binding.btnResend.setOnClickListener {
            viewModel.onIntent(EmailVerifyIntent.ResendCode(args.email))
        }

        observeState()
    }

    // ── OTP box wiring: auto-advance + backspace ───────────────────────────────

    private fun setupOtpBoxes() {
        val boxes = listOf(
            binding.otp1, binding.otp2, binding.otp3,
            binding.otp4, binding.otp5, binding.otp6
        )

        boxes.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && index < boxes.lastIndex) {
                        boxes[index + 1].requestFocus()
                    }
                    // Auto-submit once all boxes are filled
                    if (collectOtpCode().length == 6) {
                        viewModel.onIntent(
                            EmailVerifyIntent.VerifyCode(args.email, collectOtpCode())
                        )
                    }
                }
            })

            // Handle backspace to go to previous box
            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL
                    && event.action == KeyEvent.ACTION_DOWN
                    && editText.text.isNullOrEmpty()
                    && index > 0
                ) {
                    boxes[index - 1].requestFocus()
                    boxes[index - 1].setText("")
                    true
                } else {
                    false
                }
            }
        }

        // Focus first box
        boxes.first().requestFocus()
    }

    private fun collectOtpCode(): String =
        listOf(
            binding.otp1, binding.otp2, binding.otp3,
            binding.otp4, binding.otp5, binding.otp6
        ).joinToString("") { it.text?.toString().orEmpty() }

    // ── State observer ────────────────────────────────────────────────────────

    private fun observeState() {
        launchAndRepeat {
            viewModel.state.collect { state ->
                // Verify button
                binding.btnVerify.isEnabled = !state.isVerifying && !state.isSending
                binding.btnVerify.text = if (state.isVerifying)
                    getString(R.string.btn_verifying)
                else
                    getString(R.string.btn_verify)

                // Resend button
                binding.btnResend.isEnabled = !state.isCooldown && !state.isSending && !state.isVerifying
                binding.btnResend.text = if (state.isSending)
                    getString(R.string.btn_resend_sending)
                else
                    getString(R.string.btn_resend)

                // Status message (backend message on resend success)
                binding.tvStatus.isVisible = state.statusMessage != null
                binding.tvStatus.text = state.statusMessage

                // Error
                binding.tvOtpError.isVisible = state.error != null
                binding.tvOtpError.text = state.error

                // Countdown
                binding.cooldownGroup.isVisible = state.isCooldown
                if (state.isCooldown) {
                    startCountdown(state.cooldownSeconds)
                } else {
                    countDownTimer?.cancel()
                }

                // Navigation
                if (state.navigateToHome) {
                    viewModel.clearNavigation()
                    findNavController().navigate(
                        EmailVerifyFragmentDirections.actionEmailVerifyFragmentToHomeFragment()
                    )
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