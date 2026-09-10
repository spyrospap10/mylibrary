package com.example.mylibrary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class StatItem(val label: String, val value: String)

class StatAdapter : RecyclerView.Adapter<StatAdapter.StatViewHolder>() {

    private var items: List<StatItem> = emptyList()

    fun updateData(newItems: List<StatItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    class StatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLabel: TextView = view.findViewById(R.id.tvStatLabel)
        val tvValue: TextView = view.findViewById(R.id.tvStatValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stat, parent, false)
        return StatViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatViewHolder, position: Int) {
        holder.tvLabel.text = items[position].label
        holder.tvValue.text = items[position].value
    }

    override fun getItemCount() = items.size
}