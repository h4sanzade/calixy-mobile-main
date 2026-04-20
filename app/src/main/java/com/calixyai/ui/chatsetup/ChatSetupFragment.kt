package com.calixyai.ui.chatsetup

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.calixyai.R
import com.calixyai.databinding.FragmentChatSetupBinding
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
            analysisProvider = { viewModel.state.value.finalAnalysisUi },
            onEditClick = { messageId ->
                viewModel.onIntent(ChatSetupIntent.EditMessage(messageId))
            }
        )
        chipAdapter = ChipAdapter(
            onClick = { value ->
                val state = viewModel.state.value
                if (state.multiSelect)
                    viewModel.onIntent(ChatSetupIntent.ToggleMultiSelect(value))
                else
                    viewModel.onIntent(ChatSetupIntent.SelectOption(value))
            },
            isSelected = { value -> viewModel.state.value.selectedItems.contains(value) },
            isDisabled = { value ->
                // #4: disable other chips when "No Restrictions" is selected
                val noRestriction = viewModel.state.value.chips.find {
                    it.contains("No Restrictions") || it.contains("Məhdudiyyət") ||
                            it.contains("Kısıtlama") || it.contains("Без ограничений")
                }
                val noRestrictionSelected = viewModel.state.value.selectedItems.contains(noRestriction ?: "")
                noRestrictionSelected && value != noRestriction
            }
        )

        binding.recyclerMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = false }
            adapter = chatAdapter
        }
        binding.recyclerChips.adapter = chipAdapter

        // Text send
        binding.btnSend.setOnClickListener {
            val text = binding.inputMessage.text?.toString().orEmpty()
            viewModel.onIntent(ChatSetupIntent.SubmitText(text))
            binding.inputMessage.setText("")
            hideKeyboard()
        }

        // Multi-select confirm
        binding.btnContinueSelections.setOnClickListener {
            val customValue = if (viewModel.state.value.showCustomInput)
                binding.inputCustom.text?.toString() else null
            viewModel.onIntent(ChatSetupIntent.SubmitMultiSelect(customValue))
            binding.inputCustom.setText("")
            hideKeyboard()
        }

        // Slider listeners — #7: keyboard stays hidden
        binding.sliderHeight.addOnChangeListener { _, value, _ ->
            binding.tvHeightValue.text = "${value.toInt()} cm"
        }
        binding.sliderWeight.addOnChangeListener { _, value, _ ->
            binding.tvWeightValue.text = "${"%.1f".format(value)} kg"
        }

        // Slider confirm
        binding.btnConfirmSliders.setOnClickListener {
            hideKeyboard()
            val h = binding.sliderHeight.value.toInt()
            val w = binding.sliderWeight.value
            viewModel.onIntent(ChatSetupIntent.ConfirmSliders(h, w))
        }

        viewModel.onIntent(ChatSetupIntent.Initialize)

        launchAndRepeat {
            viewModel.state.collect { state ->
                chatAdapter.submitList(state.messages.toList())
                chipAdapter.submitList(state.chips)
                binding.recyclerMessages.post {
                    binding.recyclerMessages.scrollToPosition(
                        (state.messages.size - 1).coerceAtLeast(0)
                    )
                }

                // Panels
                binding.inputLayout.isVisible = state.showInput
                binding.selectionPanel.isVisible = state.chips.isNotEmpty()
                binding.btnContinueSelections.isVisible = state.multiSelect && state.chips.isNotEmpty()
                binding.inputCustom.isVisible = state.showCustomInput      // #3
                binding.sliderPanel.isVisible = state.showSliders

                if (state.finished) binding.btnUnlockPlan.isVisible = true

                // #3 & #7: keyboard control
                when {
                    state.showSliders -> hideKeyboard()   // always hide for sliders
                    state.requestKeyboard && state.showInput -> {
                        binding.inputMessage.requestFocus()
                        showKeyboard(binding.inputMessage)
                        // Reset flag after acting
                        // (flag resets via next state emission naturally)
                    }
                    state.showCustomInput -> {
                        binding.inputCustom.requestFocus()
                        showKeyboard(binding.inputCustom)
                    }
                    !state.showInput -> hideKeyboard()
                }
            }
        }

        binding.btnUnlockPlan.setOnClickListener {
            findNavController().navigate(
                ChatSetupFragmentDirections.actionChatSetupFragmentToPaymentFragment()
            )
        }
    }

    private fun showKeyboard(view: View) {
        view.post {
            val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
            imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun hideKeyboard() {
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        val focused = activity?.currentFocus ?: binding.root
        imm?.hideSoftInputFromWindow(focused.windowToken, 0)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

fun String.toSpannable(): SpannableStringBuilder {
    val sb = SpannableStringBuilder()
    var i = 0
    while (i < length) {
        if (i + 1 < length && this[i] == '*' && this[i + 1] == '*') {
            val end = indexOf("**", i + 2)
            if (end != -1) {
                val boldText = substring(i + 2, end)
                val start = sb.length
                sb.append(boldText)
                sb.setSpan(
                    StyleSpan(android.graphics.Typeface.BOLD),
                    start, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                i = end + 2
            } else { sb.append(this[i]); i++ }
        } else { sb.append(this[i]); i++ }
    }
    return sb
}