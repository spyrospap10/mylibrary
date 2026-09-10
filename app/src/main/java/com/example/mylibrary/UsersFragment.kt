package com.example.mylibrary

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.mylibrary.data.AppDatabase
import com.example.mylibrary.data.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class UsersFragment : Fragment() {

    private lateinit var db: AppDatabase
    private lateinit var userAdapter: UserAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private var currentUsersList: List<User> = emptyList()
    private var selectedRoleFilter = "Όλοι"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_users, container, false)

        db = AppDatabase.getDatabase(requireContext())

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewUsers)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        userAdapter = UserAdapter { clickedUser ->
            showUserDetailsDialog(clickedUser)
        }
        recyclerView.adapter = userAdapter

        val spinnerFilter = view.findViewById<Spinner>(R.id.spinnerRoleFilter)
        val filterOptions = arrayOf("Όλοι", "Διαχειριστές", "Χρήστες")
        spinnerFilter.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, filterOptions)

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedRoleFilter = filterOptions[position]
                applyFilter()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        loadUsers()
        return view
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            currentUsersList = db.userDao().getAllUsers()
            applyFilter()
        }
    }

    private fun applyFilter() {
        val filteredList = when (selectedRoleFilter) {
            "Διαχειριστές" -> currentUsersList.filter { it.role == "ADMIN" }
            "Χρήστες" -> currentUsersList.filter { it.role == "USER" }
            else -> currentUsersList
        }
        userAdapter.updateUsers(filteredList)
    }

    private fun showUserDetailsDialog(user: User) {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_user_details, null)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()

        dialogView.findViewById<TextView>(R.id.tvDialogName).text = "${user.lastName} ${user.firstName}"
        dialogView.findViewById<TextView>(R.id.tvDialogUsername).text = "Username: ${user.username}"
        dialogView.findViewById<TextView>(R.id.tvDialogEmail).text = "Email: ${user.email}"
        dialogView.findViewById<TextView>(R.id.tvDialogPhone).text = "Τηλέφωνο: ${user.phone}"

        val tvTotal = dialogView.findViewById<TextView>(R.id.tvDialogTotalLoans)
        val tvActive = dialogView.findViewById<TextView>(R.id.tvDialogActiveLoans)
        val tvSubscription = dialogView.findViewById<TextView>(R.id.tvDialogSubscription)

        if (user.role == "ADMIN") {
            tvTotal.visibility = View.GONE
            tvActive.visibility = View.GONE
            tvSubscription.visibility = View.GONE
            dialog.show()
        } else {
            tvTotal.visibility = View.VISIBLE
            tvActive.visibility = View.VISIBLE
            tvSubscription.visibility = View.VISIBLE

            firestore.collection("Loans")
                .whereEqualTo("username", user.username)
                .get()
                .addOnSuccessListener { loanDocs ->
                    val totalLoans = loanDocs.size()
                    var activeLoans = 0
                    for (doc in loanDocs) {
                        if (doc.getString("status") == "ACTIVE") {
                            activeLoans++
                        }
                    }
                    tvTotal.text = "Συνολικοί Δανεισμοί: $totalLoans"
                    tvActive.text = "Ενεργοί Δανεισμοί: $activeLoans"

                    firestore.collection("Subscriptions")
                        .whereEqualTo("username", user.username)
                        .whereEqualTo("status", "ACTIVE")
                        .get()
                        .addOnSuccessListener { subDocs ->
                            if (!subDocs.isEmpty) {
                                val planName = subDocs.documents[0].getString("planName") ?: ""
                                tvSubscription.text = "Συνδρομή: Ναι - $planName"
                                tvSubscription.setTextColor(android.graphics.Color.parseColor("#388E3C"))
                            } else {
                                // Δεν έχει συνδρομή
                                tvSubscription.text = "Συνδρομή: Όχι"
                                tvSubscription.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
                            }
                            dialog.show()
                        }
                        .addOnFailureListener {
                            tvSubscription.text = "Συνδρομή: Σφάλμα"
                            dialog.show()
                        }
                }
                .addOnFailureListener {
                    tvTotal.text = "Συνολικοί Δανεισμοί: Σφάλμα"
                    tvActive.text = "Ενεργοί Δανεισμοί: Σφάλμα"
                    dialog.show()
                }
        }

        dialogView.findViewById<Button>(R.id.btnDialogClose).setOnClickListener {
            dialog.dismiss()
        }
    }

}