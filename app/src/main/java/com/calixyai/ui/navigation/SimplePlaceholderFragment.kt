package com.calixyai.ui.navigation

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import com.calixyai.R
import com.calixyai.databinding.FragmentPlaceholderBinding
import com.calixyai.ui.common.BaseFragment

class SimplePlaceholderFragment : BaseFragment(R.layout.fragment_placeholder) {

    private var _binding: FragmentPlaceholderBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlaceholderBinding.bind(view)
        binding.tvTitle.text = arguments?.getString(ARG_TITLE) ?: "Placeholder"
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_TITLE = "arg_title"
        fun bundle(title: String) = bundleOf(ARG_TITLE to title)
    }
}
