package com.inscopelabs.abx.xtools.ui.category

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.inscopelabs.abx.xtools.R

class SectionedFeatureAdapter(
    private val onFeatureClick: (FeatureItem) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed class AdapterItem {
        data class Header(val title: String) : AdapterItem()
        data class Row(val item: FeatureItem) : AdapterItem()
    }

    private val items = mutableListOf<AdapterItem>()

    fun setSections(sections: List<FeatureSection>) {
        items.clear()
        for (section in sections) {
            items.add(AdapterItem.Header(section.title))
            for (featureItem in section.items) {
                items.add(AdapterItem.Row(featureItem))
            }
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AdapterItem.Header -> VIEW_TYPE_SECTION_HEADER
            is AdapterItem.Row -> VIEW_TYPE_FEATURE_ROW
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SECTION_HEADER) {
            val view = inflater.inflate(R.layout.item_section_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_feature_card, parent, false)
            RowViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is AdapterItem.Header -> (holder as HeaderViewHolder).bind(item)
            is AdapterItem.Row -> (holder as RowViewHolder).bind(item, onFeatureClick)
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.sectionHeaderTitle)

        fun bind(header: AdapterItem.Header) {
            titleTextView.text = header.title
        }
    }

    class RowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.featureIcon)
        private val titleView: TextView = itemView.findViewById(R.id.featureTitle)
        private val statusView: TextView = itemView.findViewById(R.id.featureStatus)
        private val btnOpen: MaterialButton = itemView.findViewById(R.id.btnOpen)

        fun bind(row: AdapterItem.Row, onFeatureClick: (FeatureItem) -> Unit) {
            val item = row.item
            iconView.setImageResource(item.iconRes)
            titleView.text = item.title
            statusView.text = item.statusText

            val context = itemView.context
            if (item.statusIsPositive) {
                statusView.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
            } else {
                statusView.setTextColor(ContextCompat.getColor(context, R.color.outline))
            }

            btnOpen.setOnClickListener {
                onFeatureClick(item)
            }
            itemView.setOnClickListener {
                onFeatureClick(item)
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_SECTION_HEADER = 0
        private const val VIEW_TYPE_FEATURE_ROW = 1
    }
}
