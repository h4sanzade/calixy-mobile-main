package com.calixyai.ui.chatsetup

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.calixyai.databinding.ItemOptionChipBinding

class ChipAdapter(
    private val onClick: (String) -> Unit,
    private val isSelected: (String) -> Boolean
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

    inner class ChipViewHolder(private val binding: ItemOptionChipBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: String) {
            binding.chip.text = item
            binding.chip.isChecked = isSelected(item)
            binding.chip.setOnClickListener {
                it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80).withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                }.start()
                onClick(item)
            }
        }
    }
}
