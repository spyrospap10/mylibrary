package com.example.mylibrary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mylibrary.data.AppDatabase
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: NotificationAdapter
    private var currentUsername = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        db = AppDatabase.getDatabase(requireContext())
        currentUsername = requireActivity().intent.getStringExtra("USERNAME") ?: ""
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvNotifications)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = NotificationAdapter { notification ->
            lifecycleScope.launch {
                db.notificationDao().updateNotification(notification.copy(isRead = true))
            }
        }
        recyclerView.adapter = adapter
        lifecycleScope.launch {
            db.notificationDao().getUserNotifications(currentUsername).collect { notifs ->
                adapter.updateList(notifs)
            }
        }

        return view
    }
}