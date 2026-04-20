package com.calixyai.ui.chatsetup

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.calixyai.databinding.ItemOptionChipBinding

class ChipAdapter(
    private val onClick: (String) -> Unit,
    private val isSelected: (String) -> Boolean,
    private val isDisabled: (String) -> Boolean = { false }
) : RecyclerView.Adapter<ChipAdapter.ChipViewHolder>() {

    private var items: List<String> = emptyList()

    fun submitList(newItems: List<String>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val binding = ItemOptionChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChipViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) = holder.bind(items[position])

    inner class ChipViewHolder(private val binding: ItemOptionChipBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: String) {
            val selected = isSelected(item)
            val disabled = isDisabled(item)   // #4

            binding.chip.text = item
            binding.chip.isChecked = selected
            binding.chip.isEnabled = !disabled
            binding.chip.alpha = if (disabled) 0.38f else 1f

            val bgColor = when {
                selected -> Color.parseColor("#E6F9F4")
                disabled -> Color.parseColor("#F5F5F5")
                else -> Color.parseColor("#F0F5F3")
            }
            val strokeColor = when {
                selected -> Color.parseColor("#0DBF85")
                disabled -> Color.parseColor("#E0E0E0")
                else -> Color.parseColor("#DCF0EA")
            }
            binding.chip.chipBackgroundColor = ColorStateList.valueOf(bgColor)
            binding.chip.chipStrokeColor = ColorStateList.valueOf(strokeColor)
            binding.chip.chipStrokeWidth = if (selected) 1.5f else 1f

            binding.chip.setOnClickListener {
                if (disabled) return@setOnClickListener
                it.animate().scaleX(0.93f).scaleY(0.93f).setDuration(70)
                    .withEndAction { it.animate().scaleX(1f).scaleY(1f).setDuration(70).start() }
                    .start()
                onClick(item)
                notifyDataSetChanged()  // refresh disabled states immediately
            }
        }
    }
}