package com.calixyai.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.calixyai.R
import com.calixyai.databinding.FragmentHomeBinding
import com.calixyai.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : BaseFragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        viewModel.onIntent(HomeIntent.Load)
        launchAndRepeat {
            viewModel.state.collect { state ->
                binding.tvGreeting.text = state.greeting
                binding.tvName.text = state.name
                binding.tvGoal.text = state.goal
                binding.tvBmi.text = state.bmi
                binding.tvCalories.text = state.calories
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
