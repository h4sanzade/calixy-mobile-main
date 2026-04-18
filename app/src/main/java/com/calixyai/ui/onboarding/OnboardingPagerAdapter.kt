package com.calixyai.ui.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.calixyai.databinding.ItemOnboardingPageBinding

class OnboardingPagerAdapter(
    private val pages: List<OnboardingPageUi>
) : RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemOnboardingPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PageViewHolder(binding)
    }

    override fun getItemCount(): Int = pages.size

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) = holder.bind(pages[position])

    class PageViewHolder(private val binding: ItemOnboardingPageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: OnboardingPageUi) {
            binding.tvTitle.text = item.title
            binding.tvSubtitle.text = item.subtitle
            binding.lottieView.setAnimation(item.animationRes)
            binding.lottieView.playAnimation()
        }
    }
}
