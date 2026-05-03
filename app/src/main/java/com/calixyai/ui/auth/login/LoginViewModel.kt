package com.calixyai.ui.auth.forgot

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.calixyai.R
import com.calixyai.databinding.FragmentForgotPasswordBinding
import com.calixyai.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordFragment : BaseFragment(R.layout.fragment_forgot_password) {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentForgotPasswordBinding.bind(view)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.etEmail.doAfterTextChanged {
            binding.tilEmail.error = null
            binding.tvEmailError.isVisible = false
        }

        binding.btnSendReset.setOnClickListener {
            val email = binding.etEmail.text?.toString().orEmpty().trim()
            viewModel.onIntent(ForgotPasswordIntent.SendReset(email))
        }

        binding.tvGoToLogin.setOnClickListener { findNavController().navigateUp() }
        binding.btnBackToLogin.setOnClickListener { findNavController().navigateUp() }

        binding.tvResendFromSuccess.setOnClickListener {
            val email = binding.etEmail.text?.toString().orEmpty().trim()
            viewModel.onIntent(ForgotPasswordIntent.SendReset(email))
        }

        observeState()
    }

    private fun observeState() {
        launchAndRepeat {
            viewModel.state.collect { state ->
                binding.stepRequest.isVisible = !state.showSuccess
                binding.stepSuccess.isVisible = state.showSuccess

                binding.btnSendReset.isEnabled = !state.isLoading
                binding.btnSendReset.text = if (state.isLoading)
                    getString(R.string.btn_sending)
                else
                    getString(R.string.btn_send_reset)

                binding.tvEmailError.isVisible = state.error != null
                binding.tvEmailError.text = state.error
                if (state.error != null) binding.tilEmail.error = " "
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}