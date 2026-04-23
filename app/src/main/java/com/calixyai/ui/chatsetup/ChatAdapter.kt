package com.calixyai.ui.chatsetup

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.text.toSpannable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.calixyai.databinding.ItemChatBotBinding
import com.calixyai.databinding.ItemChatTypingBinding
import com.calixyai.databinding.ItemChatUserBinding
import com.calixyai.domain.model.ChatMessage
import com.calixyai.domain.model.MessageType
import com.calixyai.domain.model.Sender
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class ChatAdapter(
    private val bmiProvider: () -> BmiUi?,
    private val analysisProvider: () -> FinalAnalysisUi?,
    private val onEditClick: (Long) -> Unit
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(Diff) {

    companion object {
        private const val TYPE_BOT = 0
        private const val TYPE_USER = 1
        private const val TYPE_TYPING = 2

        val Diff = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(a: ChatMessage, b: ChatMessage) = a.id == b.id
            override fun areContentsTheSame(a: ChatMessage, b: ChatMessage) = a == b
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when {
            item.type == MessageType.TYPING -> TYPE_TYPING
            item.sender == Sender.USER -> TYPE_USER
            else -> TYPE_BOT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> UserViewHolder(ItemChatUserBinding.inflate(inflater, parent, false))
            TYPE_TYPING -> TypingViewHolder(ItemChatTypingBinding.inflate(inflater, parent, false))
            else -> BotViewHolder(ItemChatBotBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Problem 5: determine if this is the LAST user message
        val item = getItem(position)
        val lastUserMsgId = currentList.lastOrNull { it.sender == Sender.USER }?.id

        when (holder) {
            is BotViewHolder -> holder.bind(item, bmiProvider(), analysisProvider())
            is UserViewHolder -> holder.bind(item, onEditClick, isLastUserMsg = item.id == lastUserMsgId)
            is TypingViewHolder -> holder.bind()
        }
        holder.itemView.translationY = 36f
        holder.itemView.alpha = 0f
        holder.itemView.animate()
            .translationY(0f).alpha(1f)
            .setDuration(240).setInterpolator(DecelerateInterpolator()).start()
    }

    // ── User ViewHolder ───────────────────────────────────────────────────────

    class UserViewHolder(private val binding: ItemChatUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatMessage, onEdit: (Long) -> Unit, isLastUserMsg: Boolean) {
            binding.tvMessage.text = item.text.toSpannable()

            // Problem 5: only show edit button for the last user message
            val canEdit = item.editableStep != null && isLastUserMsg
            binding.btnEdit.visibility = if (canEdit) View.VISIBLE else View.GONE
            binding.btnEdit.setOnClickListener {
                it.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80)
                    .withEndAction {
                        it.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                    }.start()
                onEdit(item.id)
            }
        }
    }

    class TypingViewHolder(private val binding: ItemChatTypingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            listOf(binding.dotOne, binding.dotTwo, binding.dotThree)
                .forEachIndexed { index, view -> animateDot(view, index * 140L) }
        }

        private fun animateDot(view: View, delay: Long) {
            view.animate().translationY(-6f).alpha(0.4f).setStartDelay(delay).setDuration(380)
                .withEndAction {
                    view.animate().translationY(0f).alpha(1f).setDuration(380)
                        .withEndAction { animateDot(view, 0L) }.start()
                }.start()
        }
    }

    class BotViewHolder(private val binding: ItemChatBotBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatMessage, bmiUi: BmiUi?, analysisUi: FinalAnalysisUi?) {
            binding.tvMessage.visibility =
                if (item.type == MessageType.TEXT) View.VISIBLE else View.GONE
            binding.cardBmi.visibility =
                if (item.type == MessageType.BMI_CARD) View.VISIBLE else View.GONE
            binding.cardAnalysis.visibility =
                if (item.type == MessageType.ANALYSIS_CARD) View.VISIBLE else View.GONE

            binding.tvMessage.text = item.text.toSpannable()

            if (item.type == MessageType.BMI_CARD && bmiUi != null) {
                binding.tvBmiValue.text = "BMI ${"%.1f".format(bmiUi.bmi)}"
                binding.tvBmiVerdict.text = when {
                    bmiUi.bmi < 18.5f -> "Underweight"
                    bmiUi.bmi < 25f -> "Healthy ✓"
                    bmiUi.bmi < 30f -> "Overweight"
                    else -> "Obese"
                }
                binding.bmiProgress.progress = bmiUi.progress
                binding.tvBmiZones.text = "Underweight · Normal · Overweight · Obese"
            }

            if (item.type == MessageType.ANALYSIS_CARD && analysisUi != null) {
                binding.tvStats.text = buildString {
                    append("Current BMI ${"%.1f".format(analysisUi.currentBmi)}   ·   Target ${"%.1f".format(analysisUi.targetBmi)}\n")
                    append("${analysisUi.estimatedDuration} months   ·   ${analysisUi.dailyCalories} kcal/day\n")
                    // Problem 4: show current → target weight
                    append("${"%.1f".format(analysisUi.currentWeight)} kg  →  ${"%.1f".format(analysisUi.targetWeight)} kg")
                }

                val entries = analysisUi.chartPoints.map { Entry(it.month, it.weight) }
                val accentColor = Color.parseColor("#0DBF85")
                val set = LineDataSet(entries, "Weight Projection").apply {
                    setDrawFilled(true)
                    lineWidth = 2.5f
                    setDrawCircles(true)
                    circleRadius = 4f
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    color = accentColor; fillColor = accentColor; fillAlpha = 30
                    setCircleColor(accentColor); circleHoleColor = Color.WHITE
                    valueTextColor = Color.TRANSPARENT
                }

                val gridColor = Color.parseColor("#DCF0EA")

                // Problem 4: horizontal limit lines for current weight and target weight
                val currentLine = LimitLine(analysisUi.currentWeight, "${"%.1f".format(analysisUi.currentWeight)} kg").apply {
                    lineColor = Color.parseColor("#0098AA")
                    lineWidth = 1f
                    textColor = Color.parseColor("#0098AA")
                    textSize = 9f
                    labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                }
                val targetLine = LimitLine(analysisUi.targetWeight, "${"%.1f".format(analysisUi.targetWeight)} kg").apply {
                    lineColor = Color.parseColor("#0DBF85")
                    lineWidth = 1f
                    textColor = Color.parseColor("#0DBF85")
                    textSize = 9f
                    labelPosition = LimitLine.LimitLabelPosition.RIGHT_BOTTOM
                }

                binding.weightChart.apply {
                    data = LineData(set)
                    setBackgroundColor(Color.TRANSPARENT)
                    axisRight.isEnabled = false
                    axisLeft.apply {
                        axisLineColor = gridColor
                        removeAllLimitLines()
                        addLimitLine(currentLine)
                        addLimitLine(targetLine)
                    }
                    xAxis.apply { position = XAxis.XAxisPosition.BOTTOM; axisLineColor = gridColor }
                    legend.isEnabled = false
                    description = Description().apply { text = "" }
                    setTouchEnabled(false)
                    animateX(900)
                    invalidate()
                }
            }
        }
    }
}