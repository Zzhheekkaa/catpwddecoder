package com.example.catpwddecoder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ConfigAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<ConfigItem>()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    fun submit(newItems: List<ConfigItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (items[position].value.isEmpty() && items[position].description.isEmpty())
            TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_header, parent, false)
                HeaderVH(v)
            }
            else -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_config, parent, false)
                ItemVH(v)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is HeaderVH -> holder.title.text = item.section
            is ItemVH -> {
                holder.desc.text = item.description
                holder.value.text = item.value.ifBlank { "—" }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvSectionTitle)
    }

    class ItemVH(view: View) : RecyclerView.ViewHolder(view) {
        val desc: TextView = view.findViewById(R.id.tvDescription)
        val value: TextView = view.findViewById(R.id.tvValue)
    }
}
