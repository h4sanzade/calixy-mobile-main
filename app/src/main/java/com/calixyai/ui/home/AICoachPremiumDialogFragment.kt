package com.calixyai.ui.home

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.fragment.app.DialogFragment
import com.calixyai.R
import com.calixyai.databinding.FragmentAiCoachPremiumDialogBinding


class AICoachPremiumDialogFragment : DialogFragment() {

    private var _binding: FragmentAiCoachPremiumDialogBinding? = null
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.CalixyAI_Dialog_Transparent)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiCoachPremiumDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val enterAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.dialog_enter)
        binding.root.startAnimation(enterAnim)

        binding.btnSetupCoach.setOnClickListener {
            parentFragmentManager.setFragmentResult(REQUEST_KEY, Bundle.EMPTY)
            dismissAllowingStateLoss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        // Override back press – do nothing (dialog is mandatory)
        dialog.setOnKeyListener { _, keyCode, _ ->
            keyCode == android.view.KeyEvent.KEYCODE_BACK
        }

        return dialog
    }

    override fun onStart() {
        super.onStart()
        val window = dialog?.window ?: return

        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.attributes = window.attributes.also { lp ->
            lp.dimAmount = 0.55f

            val displayMetrics = resources.displayMetrics
            lp.width = (displayMetrics.widthPixels * 0.90).toInt()
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT

            lp.windowAnimations = R.style.CalixyAI_DialogAnimation
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }


    companion object {
        const val TAG = "ai_coach_premium_dialog"
        const val REQUEST_KEY = "ai_coach_premium_continue"
    }
}