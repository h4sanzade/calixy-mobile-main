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
    }

    override fun getItemViewType(position: Int): Int =
        if (position == 1) TYPE_PAGE2 else TYPE_DEFAULT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_PAGE2 -> Page2ViewHolder(
                ItemOnboardingTwoBinding.inflate(inflater, parent, false)
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
            is PageViewHolder  -> holder.bind(page)
        }
    }

    // ── Default ViewHolder (pages 0 & 2) ─────────────────────────────────────

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


    class Page2ViewHolder(
        private val binding: ItemOnboardingTwoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(page: OnboardingPage) {
            binding.tvTag.text      = page.tag
            binding.tvTitle.text    = page.title
            binding.tvSubtitle.text = page.subtitle

        }
    }
}