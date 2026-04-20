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
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class ChatAdapter(
    private val bmiProvider: () -> BmiUi?,
    private val analysisProvider: () -> FinalAnalysisUi?
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(Diff) {

    companion object {
        private const val TYPE_BOT = 0
        private const val TYPE_USER = 1
        private const val TYPE_TYPING = 2

        val Diff = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
                oldItem == newItem
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
        when (holder) {
            is BotViewHolder -> holder.bind(getItem(position), bmiProvider(), analysisProvider())
            is UserViewHolder -> holder.bind(getItem(position))
            is TypingViewHolder -> holder.bind()
        }
        // Slide-in animation
        holder.itemView.translationY = 36f
        holder.itemView.alpha = 0f
        holder.itemView.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(240)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    class UserViewHolder(private val binding: ItemChatUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessage) {
            // User messages can also contain **bold** (e.g. height/weight confirm)
            binding.tvMessage.text = item.text.toSpannable()
        }
    }

    class TypingViewHolder(private val binding: ItemChatTypingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            listOf(binding.dotOne, binding.dotTwo, binding.dotThree)
                .forEachIndexed { index, view -> animateDot(view, index * 140L) }
        }

        private fun animateDot(view: View, delay: Long) {
            view.animate()
                .translationY(-6f).alpha(0.4f)
                .setStartDelay(delay).setDuration(380)
                .withEndAction {
                    view.animate()
                        .translationY(0f).alpha(1f).setDuration(380)
                        .withEndAction { animateDot(view, 0L) }
                        .start()
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

            // Render bold markdown in bot text
            binding.tvMessage.text = item.text.toSpannable()

            if (item.type == MessageType.BMI_CARD && bmiUi != null) {
                binding.tvBmiValue.text = "BMI ${"%.1f".format(bmiUi.bmi)}"
                val verdictShort = when {
                    bmiUi.bmi < 18.5f -> "Underweight"
                    bmiUi.bmi < 25f -> "Healthy ✓"
                    bmiUi.bmi < 30f -> "Overweight"
                    else -> "Obese"
                }
                binding.tvBmiVerdict.text = verdictShort
                binding.bmiProgress.progress = bmiUi.progress
                binding.tvBmiZones.text = "Underweight · Normal · Overweight · Obese"
            }

            if (item.type == MessageType.ANALYSIS_CARD && analysisUi != null) {
                binding.tvStats.text = buildString {
                    append("Current BMI ${"%.1f".format(analysisUi.currentBmi)}   ·   Target ${"%.1f".format(analysisUi.targetBmi)}\n")
                    append("${analysisUi.estimatedDuration} months   ·   ${analysisUi.dailyCalories} kcal/day")
                }

                val entries = analysisUi.chartPoints.map { Entry(it.month, it.weight) }
                val accentColor = Color.parseColor("#0DBF85")
                val set = LineDataSet(entries, "Weight Projection").apply {
                    setDrawFilled(true)
                    lineWidth = 2.5f
                    setDrawCircles(true)
                    circleRadius = 4f
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    color = accentColor
                    fillColor = accentColor
                    fillAlpha = 30
                    setCircleColor(accentColor)
                    circleHoleColor = Color.WHITE
                    valueTextColor = Color.TRANSPARENT
                }

                val gridColor = Color.parseColor("#DCF0EA")
                binding.weightChart.apply {
                    data = LineData(set)
                    setBackgroundColor(Color.TRANSPARENT)
                    axisRight.isEnabled = false
                    axisLeft.axisLineColor = gridColor
                    xAxis.apply {
                        position = XAxis.XAxisPosition.BOTTOM
                        axisLineColor = gridColor
                    }
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