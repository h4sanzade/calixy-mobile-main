package com.calixyai.ui.auth.register

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.calixyai.R
import com.calixyai.databinding.FragmentRegisterBinding
import com.calixyai.ui.common.BaseFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : BaseFragment(R.layout.fragment_register) {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegisterViewModel by viewModels()

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    viewModel.onIntent(RegisterIntent.GoogleSignUp(idToken))
                }
            } catch (e: ApiException) {
                // User cancelled or Google error — silent fail
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
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
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

                binding.btnGoogle.isEnabled = !state.isLoading

                binding.tvRegisterError.isVisible = state.error != null
                binding.tvRegisterError.text = state.error

                when (state.errorField) {
                    RegisterErrorField.EMAIL -> binding.tilEmail.error = " "
                    RegisterErrorField.PASSWORD -> binding.tilPassword.error = " "
                    RegisterErrorField.CONFIRM -> binding.tilConfirmPassword.error = " "
                    null -> Unit
                }

                if (state.navigateToVerify) {
                    viewModel.clearNavigation()
                    findNavController().navigate(
                        RegisterFragmentDirections.actionRegisterFragmentToEmailVerifyFragment(
                            email = binding.etEmail.text?.toString().orEmpty().trim()
                        )
                    )
                }

                if (state.navigateToHome) {
                    viewModel.clearNavigation()
                    findNavController().navigate(
                        RegisterFragmentDirections.actionRegisterFragmentToHomeFragment()
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