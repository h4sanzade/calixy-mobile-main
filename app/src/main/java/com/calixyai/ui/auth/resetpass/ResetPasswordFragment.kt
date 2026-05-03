package com.calixyai.ui.auth.resetpass

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.calixyai.R
import com.calixyai.databinding.FragmentResetPasswordBinding
import com.calixyai.ui.auth.reset.ResetPasswordFragmentArgs
import com.calixyai.ui.auth.reset.ResetPasswordFragmentDirections
import com.calixyai.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ResetPasswordFragment : BaseFragment(R.layout.fragment_reset_password) {

    private var _binding: FragmentResetPasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ResetPasswordViewModel by viewModels()
    private val args: ResetPasswordFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentResetPasswordBinding.bind(view)

        setupInputListeners()
        setupClickListeners()
        observeState()
    }

    private fun setupInputListeners() {
        binding.etCode.doAfterTextChanged { binding.tvError.isVisible = false }
        binding.etNewPassword.doAfterTextChanged { binding.tvError.isVisible = false }
        binding.etConfirmPassword.doAfterTextChanged { binding.tvError.isVisible = false }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnResetPassword.setOnClickListener {
            val code = binding.etCode.text?.toString().orEmpty().trim()
            val newPassword = binding.etNewPassword.text?.toString().orEmpty()
            val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()
            viewModel.onIntent(
                ResetPasswordIntent.Submit(
                    email = args.email,
                    code = code,
                    newPassword = newPassword,
                    confirmPassword = confirmPassword
                )
            )
        }
    }

    private fun observeState() {
        launchAndRepeat {
            viewModel.state.collect { state ->
                binding.btnResetPassword.isEnabled = !state.isLoading
                binding.btnResetPassword.text = if (state.isLoading)
                    getString(R.string.btn_resetting)
                else
                    getString(R.string.btn_reset_password)

                // Error — exact message from backend
                binding.tvError.isVisible = state.error != null
                binding.tvError.text = state.error

                if (state.navigateToLogin) {
                    viewModel.clearNavigation()
                    // Navigate to Login, clearing the back stack up to Login
                    findNavController().navigate(
                        ResetPasswordFragmentDirections
                            .actionResetPasswordFragmentToLoginFragment()
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