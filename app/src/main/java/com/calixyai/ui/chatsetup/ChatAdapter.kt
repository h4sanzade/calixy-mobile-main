package com.calixyai.ui.chatsetup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
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
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean = oldItem == newItem
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
        holder.itemView.translationY = 40f
        holder.itemView.alpha = 0f
        holder.itemView.animate().translationY(0f).alpha(1f).setDuration(250).setInterpolator(DecelerateInterpolator()).start()
    }

    class UserViewHolder(private val binding: ItemChatUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessage) { binding.tvMessage.text = item.text }
    }

    class TypingViewHolder(private val binding: ItemChatTypingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            listOf(binding.dotOne, binding.dotTwo, binding.dotThree).forEachIndexed { index, view ->
                view.animate().scaleX(1.35f).scaleY(1.35f).alpha(0.55f)
                    .setStartDelay(index * 120L).setDuration(420).withEndAction {
                        view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(420).start()
                    }.start()
            }
        }
    }

    class BotViewHolder(private val binding: ItemChatBotBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessage, bmiUi: BmiUi?, analysisUi: FinalAnalysisUi?) {
            binding.tvMessage.visibility = if (item.type == MessageType.TEXT) View.VISIBLE else View.GONE
            binding.cardBmi.visibility = if (item.type == MessageType.BMI_CARD) View.VISIBLE else View.GONE
            binding.cardAnalysis.visibility = if (item.type == MessageType.ANALYSIS_CARD) View.VISIBLE else View.GONE
            binding.tvMessage.text = item.text
            if (item.type == MessageType.BMI_CARD && bmiUi != null) {
                binding.tvBmiValue.text = "BMI ${bmiUi.bmi}"
                binding.tvBmiVerdict.text = bmiUi.verdict
                binding.bmiProgress.progress = bmiUi.progress
                binding.tvBmiZones.text = "Underweight · Normal · Overweight · Obese"
            }
            if (item.type == MessageType.ANALYSIS_CARD && analysisUi != null) {
                binding.tvStats.text = "Current BMI ${analysisUi.currentBmi}   ·   Target BMI ${analysisUi.targetBmi}\n${analysisUi.estimatedDuration} months   ·   ${analysisUi.dailyCalories} kcal/day"
                val entries = analysisUi.chartPoints.map { Entry(it.month, it.weight) }
                val set = LineDataSet(entries, "Weight Projection").apply {
                    setDrawFilled(true)
                    lineWidth = 3f
                    setDrawCircles(true)
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    color = android.graphics.Color.parseColor("#7C4DFF")
                    fillColor = android.graphics.Color.parseColor("#332A6DFF")
                    circleColor = android.graphics.Color.parseColor("#00E5FF")
                    valueTextColor = android.graphics.Color.TRANSPARENT
                }
                binding.weightChart.apply {
                    data = LineData(set)
                    axisRight.isEnabled = false
                    axisLeft.textColor = android.graphics.Color.WHITE
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.textColor = android.graphics.Color.WHITE
                    legend.isEnabled = false
                    description = Description().apply { text = "" }
                    animateX(900)
                    invalidate()
                }
            }
        }
    }
}
