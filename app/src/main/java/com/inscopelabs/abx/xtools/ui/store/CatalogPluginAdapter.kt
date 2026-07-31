package com.inscopelabs.abx.xtools.ui.store

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.inscopelabs.abx.xtools.R
import com.inscopelabs.abx.xtools.plugin.catalog.CatalogPlugin

class CatalogPluginAdapter(
    private val onItemClick: (CatalogPlugin) -> Unit
) : RecyclerView.Adapter<CatalogPluginAdapter.PluginViewHolder>() {

    private val plugins = mutableListOf<CatalogPlugin>()

    fun submitList(newList: List<CatalogPlugin>) {
        plugins.clear()
        plugins.addAll(newList)
        notifyDataSetChanged()
    }

    fun appendList(additionalList: List<CatalogPlugin>) {
        val startPosition = plugins.size
        plugins.addAll(additionalList)
        notifyItemRangeInserted(startPosition, additionalList.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PluginViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_catalog_plugin, parent, false)
        return PluginViewHolder(view)
    }

    override fun onBindViewHolder(holder: PluginViewHolder, position: Int) {
        holder.bind(plugins[position])
    }

    override fun getItemCount(): Int = plugins.size

    inner class PluginViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvPluginName)
        private val tvVersion: TextView = itemView.findViewById(R.id.tvPluginVersion)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvPluginDescription)

        fun bind(plugin: CatalogPlugin) {
            tvName.text = plugin.name
            tvVersion.text = "v${plugin.version}"
            tvDescription.text = if (!plugin.description.isNullOrBlank()) plugin.description else "No description"

            itemView.setOnClickListener {
                onItemClick(plugin)
            }
        }
    }
}
