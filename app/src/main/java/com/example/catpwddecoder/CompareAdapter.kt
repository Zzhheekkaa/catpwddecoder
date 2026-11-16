package com.example.catpwddecoder

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class CompareAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var hideMatches: Boolean = false
    private val allRows = mutableListOf<CompareRow>()
    private val visibleRows = mutableListOf<CompareRow>()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ROW = 1
    }

    fun submit(rows: List<CompareRow>) {
        allRows.clear()
        allRows.addAll(rows)
        rebuildVisible()
    }

    fun setHideMatches(hide: Boolean) {
        if (hideMatches == hide) return
        hideMatches = hide
        rebuildVisible()
    }

    private fun rebuildVisible() {
        visibleRows.clear()
        var lastSection: String? = null
        for (r in allRows) {
            if (hideMatches && r.isMatch) continue
            if (r.section != lastSection) {
                visibleRows.add(r.copy(description = r.section, valueLeft = null, valueRight = null))
                lastSection = r.section
            }
            visibleRows.add(r)
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val r = visibleRows[position]
        return if (r.valueLeft == null && r.valueRight == null && r.description == r.section)
            TYPE_HEADER else TYPE_ROW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderVH(inflater.inflate(R.layout.item_compare_header, parent, false))
        } else {
            RowVH(inflater.inflate(R.layout.item_compare_row, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val r = visibleRows[position]
        when (holder) {
            is HeaderVH -> holder.title.text = r.section
            is RowVH -> {
                val ctx = holder.itemView.context
                holder.desc.text = r.description
                holder.left.text = r.valueLeft ?: "—"
                holder.right.text = r.valueRight ?: "—"

                val bg = if (r.isMatch) Color.TRANSPARENT
                else ContextCompat.getColor(ctx, android.R.color.holo_red_light)
                holder.itemView.setBackgroundColor(bg)

                holder.desc.setTextColor(Color.WHITE)
                holder.left.setTextColor(Color.WHITE)
                holder.right.setTextColor(Color.WHITE)
            }
        }
    }

    override fun getItemCount(): Int = visibleRows.size

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvSection)
    }

    class RowVH(view: View) : RecyclerView.ViewHolder(view) {
        val desc: TextView = view.findViewById(R.id.tvDescription)
        val left: TextView = view.findViewById(R.id.tvValueLeft)
        val right: TextView = view.findViewById(R.id.tvValueRight)
    }
}
