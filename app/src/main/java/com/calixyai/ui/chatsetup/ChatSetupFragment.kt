package com.calixyai.ui.chatsetup

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.calixyai.R
import com.calixyai.databinding.FragmentChatSetupBinding
import com.calixyai.domain.model.ChatStep
import com.calixyai.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatSetupFragment : BaseFragment(R.layout.fragment_chat_setup) {

    private var _binding: FragmentChatSetupBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatSetupViewModel by viewModels()

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var chipAdapter: ChipAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChatSetupBinding.bind(view)

        chatAdapter = ChatAdapter(
            bmiProvider = { viewModel.state.value.bmiUi },
            analysisProvider = { viewModel.state.value.finalAnalysisUi }
        )
        chipAdapter = ChipAdapter(
            onClick = { value ->
                val state = viewModel.state.value
                if (state.multiSelect) viewModel.onIntent(ChatSetupIntent.ToggleMultiSelect(value)) else viewModel.onIntent(ChatSetupIntent.SelectOption(value))
            },
            isSelected = { value -> viewModel.state.value.selectedItems.contains(value) }
        )

        binding.recyclerMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = false }
            adapter = chatAdapter
        }
        binding.recyclerChips.adapter = chipAdapter

        binding.btnSend.setOnClickListener {
            val text = binding.inputMessage.text?.toString().orEmpty()
            viewModel.onIntent(ChatSetupIntent.SubmitText(text))
            binding.inputMessage.setText("")
        }
        binding.btnContinueSelections.setOnClickListener {
            val customValue = if (viewModel.state.value.selectedItems.contains("✏️ Custom…")) binding.inputCustom.text?.toString() else null
            viewModel.onIntent(ChatSetupIntent.SubmitMultiSelect(customValue))
            binding.inputCustom.setText("")
        }

        viewModel.onIntent(ChatSetupIntent.Initialize)

        launchAndRepeat {
            viewModel.state.collect { state ->
                chatAdapter.submitList(state.messages)
                chipAdapter.submitList(state.chips)
                binding.recyclerMessages.scrollToPosition((state.messages.size - 1).coerceAtLeast(0))
                binding.inputLayout.isVisible = state.showInput
                binding.selectionPanel.isVisible = state.chips.isNotEmpty()
                binding.btnContinueSelections.isVisible = state.multiSelect && state.chips.isNotEmpty()
                binding.inputCustom.isVisible = state.selectedItems.contains("✏️ Custom…")
                if (state.finished && state.step == ChatStep.COMPLETE) {
                    binding.btnUnlockPlan.isVisible = true
                }
            }
        }

        binding.btnUnlockPlan.setOnClickListener {
            findNavController().navigate(ChatSetupFragmentDirections.actionChatSetupFragmentToPaymentFragment())
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
