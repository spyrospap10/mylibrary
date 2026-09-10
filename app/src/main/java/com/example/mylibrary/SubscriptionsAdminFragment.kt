package com.example.mylibrary

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SubscriptionsAdminFragment : Fragment() {
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var tvEarnings: TextView
    private lateinit var tvEmptySubs: TextView
    private lateinit var rvAdminSubscriptions: RecyclerView
    private lateinit var adapter: AdminSubscriptionAdapter
    private var activeList = mutableListOf<Map<String, Any>>()
    private var historyList = mutableListOf<Map<String, Any>>()
    private lateinit var btnActive: Button
    private lateinit var btnHistory: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_subscriptions_admin, container, false)

        tvEarnings = view.findViewById(R.id.tvTotalEarnings)
        tvEmptySubs = view.findViewById(R.id.tvEmptySubs)
        rvAdminSubscriptions = view.findViewById(R.id.rvAdminSubscriptions)
        btnActive = view.findViewById(R.id.btnActiveSubs)
        btnHistory = view.findViewById(R.id.btnHistorySubs)

        rvAdminSubscriptions.layoutManager = LinearLayoutManager(requireContext())
        adapter = AdminSubscriptionAdapter()
        rvAdminSubscriptions.adapter = adapter

        btnActive.setOnClickListener {
            btnActive.setBackgroundColor(Color.parseColor("#3F51B5"))
            btnHistory.setBackgroundColor(Color.parseColor("#9E9E9E"))
            updateUI(activeList, false)
        }

        btnHistory.setOnClickListener {
            btnHistory.setBackgroundColor(Color.parseColor("#3F51B5"))
            btnActive.setBackgroundColor(Color.parseColor("#9E9E9E"))
            updateUI(historyList, true)
        }

        loadFinancials()
        return view
    }

    private fun loadFinancials() {
        firestore.collection("Subscriptions")
            .get()
            .addOnSuccessListener { documents ->
                activeList.clear()
                historyList.clear()
                var total = 0.0

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val todayStr = sdf.format(Date())
                val today = sdf.parse(todayStr)

                for (doc in documents) {
                    val data = doc.data
                    total += doc.getDouble("price") ?: 0.0

                    val status = data["status"] as? String ?: "ACTIVE"
                    val endDateStr = data["endDate"] as? String ?: ""
                    val endDate = try { sdf.parse(endDateStr) } catch (e: Exception) { null }

                    if (status == "EXPIRED" || (endDate != null && endDate.before(today))) {
                        historyList.add(data)
                    } else {
                        activeList.add(data)
                    }
                }

                tvEarnings.text = "Συνολικά Έσοδα: ${String.format(Locale.US, "%.2f", total)}€"
                updateUI(activeList, false)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Σφάλμα κατά τη φόρτωση", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUI(list: List<Map<String, Any>>, isHistory: Boolean) {
        adapter.updateList(list, historyMode = isHistory)

        if (list.isEmpty()) {
            rvAdminSubscriptions.visibility = View.GONE
            tvEmptySubs.visibility = View.VISIBLE
            tvEmptySubs.text = if (isHistory) "Δεν υπάρχουν παλιές συνδρομές." else "Δεν υπάρχουν ενεργές συνδρομές."
        } else {
            rvAdminSubscriptions.visibility = View.VISIBLE
            tvEmptySubs.visibility = View.GONE
        }
    }
}