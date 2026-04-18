package com.calixyai.ui.payment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.calixyai.R
import com.calixyai.databinding.FragmentPaymentBinding
import com.calixyai.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PaymentFragment : BaseFragment(R.layout.fragment_payment) {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PaymentViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPaymentBinding.bind(view)

        binding.btnStartTrial.setOnClickListener { viewModel.onIntent(PaymentIntent.StartTrial) }

        launchAndRepeat {
            viewModel.state.collect { state ->
                if (state.navigateHome) {
                    findNavController().navigate(PaymentFragmentDirections.actionPaymentFragmentToHomeFragment())
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
