package com.calixyai.ui.auth.login

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.calixyai.R
import com.calixyai.databinding.FragmentLoginBinding
import com.calixyai.ui.common.BaseFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : BaseFragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    viewModel.onIntent(LoginIntent.GoogleSignIn(idToken))
                } else {
                    // idToken null — shouldn't happen with requestIdToken configured
                }
            } catch (e: ApiException) {
                // Google sign-in failed; no action needed as user cancelled or error occurred
            }
        }
    }

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(requireActivity(), gso)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        setupInputListeners()
        setupClickListeners()
        observeState()
    }

    private fun setupInputListeners() {
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
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        }
    }

    private fun observeState() {
        launchAndRepeat {
            viewModel.state.collect { state ->
                // Button state
                binding.btnLogin.isEnabled = !state.isLoading
                binding.btnLogin.text = if (state.isLoading)
                    getString(R.string.btn_login_loading)
                else
                    getString(R.string.btn_login)

                // Google button mirrors loading too
                binding.btnGoogle.isEnabled = !state.isLoading

                // Error display — exact message from backend
                binding.tvLoginError.isVisible = state.error != null
                binding.tvLoginError.text = state.error
                if (state.error != null) {
                    binding.tilEmail.error = " "
                    binding.tilPassword.error = " "
                }

                // Navigation
                if (state.navigateToHome) {
                    viewModel.clearNavigation()
                    findNavController().navigate(
                        LoginFragmentDirections.actionLoginFragmentToHomeFragment()
                    )
                }

                if (state.navigateToVerify) {
                    viewModel.clearNavigation()
                    findNavController().navigate(
                        LoginFragmentDirections.actionLoginFragmentToEmailVerifyFragment(
                            email = binding.etEmail.text?.toString().orEmpty().trim()
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