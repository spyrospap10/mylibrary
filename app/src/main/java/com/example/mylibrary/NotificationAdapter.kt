package com.example.mylibrary

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mylibrary.data.Notification

class NotificationAdapter(private val onNotificationClick: (Notification) -> Unit) : RecyclerView.Adapter<NotificationAdapter.NotifViewHolder>() {

    private var notifList: List<Notification> = emptyList()

    fun updateList(newList: List<Notification>) {
        notifList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return NotifViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
        val notif = notifList[position]
        holder.tvMessage.text = notif.message
        holder.tvDate.text = notif.date

        if (!notif.isRead) {
            holder.tvMessage.setTypeface(null, Typeface.BOLD)
            holder.tvMessage.setTextColor(Color.BLACK)
        } else {
            holder.tvMessage.setTypeface(null, Typeface.NORMAL)
            holder.tvMessage.setTextColor(Color.GRAY)
        }

        holder.itemView.setOnClickListener {
            onNotificationClick(notif)
        }
    }

    override fun getItemCount() = notifList.size

    class NotifViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(android.R.id.text1)
        val tvDate: TextView = view.findViewById(android.R.id.text2)
    }
}