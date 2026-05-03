package com.calixyai.ui.auth.login

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.calixyai.R
import com.calixyai.databinding.FragmentLoginBinding
import com.calixyai.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : BaseFragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        setupInputListeners()
        setupClickListeners()
        observeState()
    }

    private fun setupInputListeners() {
        // Clear error as user types
        binding.etEmail.doAfterTextChanged {
            binding.tilEmail.error = null
            binding.tvLoginError.isVisible = false
        }
        binding.etPassword.doAfterTextChanged {
            binding.tilPassword.error = null
            binding.tvLoginError.isVisible = false
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString().orEmpty().trim()
            val password = binding.etPassword.text?.toString().orEmpty()
            viewModel.onIntent(LoginIntent.Submit(email, password))
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(
                LoginFragmentDirections.actionLoginFragmentToForgotPasswordFragment()
            )
        }

        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigate(
                LoginFragmentDirections.actionLoginFragmentToRegisterFragment()
            )
        }

        binding.btnGoogle.setOnClickListener {
            viewModel.onIntent(LoginIntent.SignInWithGoogle)
        }
    }

    private fun observeState() {
        launchAndRepeat {
            viewModel.state.collect { state ->
                binding.btnLogin.isEnabled = !state.isLoading
                binding.btnLogin.text = if (state.isLoading) "Logging in…" else "Log In"

                binding.tvLoginError.isVisible = state.error != null
                binding.tvLoginError.text = state.error

                if (state.error != null) {
                    binding.tilEmail.error = " "     // show red border
                    binding.tilPassword.error = " "
                }

                if (state.navigateToHome) {
                    findNavController().navigate(
                        LoginFragmentDirections.actionLoginFragmentToHomeFragment()
                    )
                }

                if (state.navigateToVerify) {
                    findNavController().navigate(
                        LoginFragmentDirections.actionLoginFragmentToEmailVerifyFragment(
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
