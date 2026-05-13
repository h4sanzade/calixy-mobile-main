package com.calixyai.ui.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.calixyai.databinding.ItemOnboardingPageBinding
import com.calixyai.databinding.ItemOnboardingTwoBinding

class OnboardingPagerAdapter(
    private val pages: List<OnboardingPage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_DEFAULT = 0
        private const val TYPE_PAGE2   = 1
        private const val TYPE_PAGE3   = 2
    }

    override fun getItemViewType(position: Int): Int = when (position) {
        1    -> TYPE_PAGE2
        2    -> TYPE_PAGE3
        else -> TYPE_DEFAULT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_PAGE2 -> Page2ViewHolder(
                ItemOnboardingTwoBinding.inflate(inflater, parent, false)
            )
            TYPE_PAGE3 -> Page3ViewHolder(
                ItemOnboardingThreeBinding.inflate(inflater, parent, false)
            )
            else -> PageViewHolder(
                ItemOnboardingPageBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun getItemCount() = pages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val page = pages[position]
        when (holder) {
            is Page2ViewHolder -> holder.bind(page)
            is Page3ViewHolder -> holder.bind(page)
            is PageViewHolder  -> holder.bind(page)
        }
    }

    // ── Default ViewHolder (page 0) ───────────────────────────────────────────
    class PageViewHolder(
        private val binding: ItemOnboardingPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(page: OnboardingPage) {
            binding.tvTag.text      = page.tag
            binding.tvTitle.text    = page.title
            binding.tvSubtitle.text = page.subtitle
            binding.animView.pageIndex = page.pageIndex
        }
    }

    // ── Page 2 ViewHolder ─────────────────────────────────────────────────────
    class Page2ViewHolder(
        private val binding: ItemOnboardingTwoBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(page: OnboardingPage) {
            binding.tvTag.text      = page.tag
            binding.tvTitle.text    = page.title
            binding.tvSubtitle.text = page.subtitle
        }
    }

    // ── Page 3 ViewHolder (meal analysis animation) ───────────────────────────
    class Page3ViewHolder(
        private val binding: ItemOnboardingThreeBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(page: OnboardingPage) {
            binding.tvTag.text      = page.tag
            binding.tvTitle.text    = page.title
            binding.tvSubtitle.text = page.subtitle

            binding.mealAnalysisView.onSceneChanged = { scene ->
                when (scene) {
                    2 -> {
                        binding.tvTag.text      = "Step 2 of 3"
                        binding.tvTitle.text    = "AI reads every\nnutrient."
                        binding.tvSubtitle.text = "Macros, calories, and ingredients identified instantly. No manual logging."
                    }
                    3 -> {
                        binding.tvTag.text      = "Step 3 of 3"
                        binding.tvTitle.text    = "Your coach\nresponds."
                        binding.tvSubtitle.text = "Personalized recovery advice, portion guidance, and wellness insights — in seconds."
                    }
                }
            }
        }
    }
}