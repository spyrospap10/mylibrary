package com.example.mylibrary

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdminSubscriptionAdapter : RecyclerView.Adapter<AdminSubscriptionAdapter.SubViewHolder>() {

    private var subList: List<Map<String, Any>> = emptyList()
    private var isHistoryMode: Boolean = false
    fun updateList(newList: List<Map<String, Any>>, historyMode: Boolean) {
        subList = newList
        isHistoryMode = historyMode
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_subscription_admin, parent, false)
        return SubViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubViewHolder, position: Int) {
        val sub = subList[position]
        holder.tvUser.text = "Χρήστης: ${sub["username"]}"
        holder.tvPlan.text = "Πακέτο: ${sub["planName"]} (${sub["price"]}€)"

        if (isHistoryMode) {
            holder.tvDates.text = "Έληξε: ${sub["endDate"]}"
            holder.tvDates.setTextColor(Color.parseColor("#D32F2F"))
        } else {
            holder.tvDates.text = "Λήξη: ${sub["endDate"]}"
            holder.tvDates.setTextColor(Color.parseColor("#388E3C"))
        }
    }

    override fun getItemCount() = subList.size

    class SubViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUser: TextView = view.findViewById(R.id.tvAdminSubUser)
        val tvPlan: TextView = view.findViewById(R.id.tvAdminSubPlan)
        val tvDates: TextView = view.findViewById(R.id.tvAdminSubDates)
    }
}