package com.calixyai.ui.home

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.calixyai.databinding.FragmentAiCoachDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
class AICoachDialogFragment : DialogFragment() {

    private var _binding: FragmentAiCoachDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false

        _binding = FragmentAiCoachDialogBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()

        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        binding.btnContinue.setOnClickListener {
            parentFragmentManager.setFragmentResult(REQUEST_KEY, Bundle.EMPTY)
            dismiss()
        }

        return dialog
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "ai_coach_dialog"
        const val REQUEST_KEY = "ai_coach_continue"
    }
}