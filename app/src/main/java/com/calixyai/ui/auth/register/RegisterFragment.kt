package com.calixyai.ui.auth.register

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.calixyai.R
import com.calixyai.databinding.FragmentRegisterBinding
import com.calixyai.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : BaseFragment(R.layout.fragment_register) {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegisterViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)

        setupInputListeners()
        setupClickListeners()
        observeState()
    }

    private fun setupInputListeners() {
        binding.etEmail.doAfterTextChanged {
            binding.tilEmail.error = null
            binding.tvRegisterError.isVisible = false
        }
        binding.etPassword.doAfterTextChanged {
            binding.tilPassword.error = null
            binding.tvRegisterError.isVisible = false
        }
        binding.etConfirmPassword.doAfterTextChanged {
            binding.tilConfirmPassword.error = null
            binding.tvRegisterError.isVisible = false
        }
    }

    private fun setupClickListeners() {
        binding.btnSignUp.setOnClickListener {
            val email = binding.etEmail.text?.toString().orEmpty().trim()
            val password = binding.etPassword.text?.toString().orEmpty()
            val confirm = binding.etConfirmPassword.text?.toString().orEmpty()
            viewModel.onIntent(RegisterIntent.Submit(email, password, confirm))
        }

        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnGoogle.setOnClickListener {
            viewModel.onIntent(RegisterIntent.SignUpWithGoogle)
        }
    }

    private fun observeState() {
        launchAndRepeat {
            viewModel.state.collect { state ->
                binding.btnSignUp.isEnabled = !state.isLoading
                binding.btnSignUp.text = if (state.isLoading)
                    getString(R.string.btn_signup_loading)
                else
                    getString(R.string.btn_signup)

                binding.tvRegisterError.isVisible = state.error != null
                binding.tvRegisterError.text = state.error

                when (state.errorField) {
                    RegisterErrorField.EMAIL -> binding.tilEmail.error = " "
                    RegisterErrorField.PASSWORD -> binding.tilPassword.error = " "
                    RegisterErrorField.CONFIRM -> binding.tilConfirmPassword.error = " "
                    null -> Unit
                }

                if (state.navigateToVerify) {
                    findNavController().navigate(
                        RegisterFragmentDirections.actionRegisterFragmentToEmailVerifyFragment(
                            email = binding.etEmail.text?.toString().orEmpty()
                        )
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